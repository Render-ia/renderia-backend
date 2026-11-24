package com.renderia.renderia_backend.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.renderia.renderia_backend.ai.AiServiceClient;
import com.renderia.renderia_backend.dto.AiServiceResponse;
import com.renderia.renderia_backend.dto.AnalysisResponse;
import com.renderia.renderia_backend.dto.DetectedElement;
import com.renderia.renderia_backend.dto.ElementInput;
import com.renderia.renderia_backend.dto.Model3DRequest;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.AiAnalysis;
import com.renderia.renderia_backend.model.AiModel;
import com.renderia.renderia_backend.model.AnalysisStatus;
import com.renderia.renderia_backend.model.CatalogEntity;
import com.renderia.renderia_backend.model.FloorPlan;
import com.renderia.renderia_backend.repository.AiAnalysisRepository;
import com.renderia.renderia_backend.repository.ElementTypeRepository;
import com.renderia.renderia_backend.repository.MaterialRepository;
import com.renderia.renderia_backend.security.CurrentUser;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ciclo completo de un análisis, orquestado por el backend:
 * usuario → frontend → backend → BD → IA → backend → BD → frontend.
 *
 * Patrón Facade: el controlador hace UNA llamada (run) y esta clase coordina
 * los subsistemas: permisos y estado (AnalysisService), plano (FloorPlanService),
 * servicio de IA (AiServiceClient), elementos y modelo 3D (Model3DService) y bitácora.
 *
 * Patrón State: los cambios de estado pasan por AnalysisStatus.canMoveTo
 * (PENDING → PROCESSING → COMPLETED o FAILED).
 *
 * La llamada a la IA va FUERA de una transacción: puede tardar mientras el servicio
 * despierta y no debe tener ocupada una conexión de la base. Antes y después sí hay
 * transacciones cortas, así que si la IA falla el análisis queda en FAILED (nunca
 * "atascado" en PROCESSING) aunque el usuario cierre el navegador.
 */
@Service
public class AnalysisPipelineService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisPipelineService.class);
    private static final ObjectMapper JSON = JsonMapper.builder().build();
    static final int MAX_ERROR_CHARS = 2000;

    private final AnalysisService analysisService;
    private final AiAnalysisRepository analyses;
    private final FloorPlanService floorPlans;
    private final Model3DService models3d;
    private final ElementTypeRepository elementTypes;
    private final MaterialRepository materials;
    private final AiServiceClient aiService;
    private final ActivityLogService activity;
    private final TransactionTemplate tx;

    public AnalysisPipelineService(AnalysisService analysisService, AiAnalysisRepository analyses,
                                   FloorPlanService floorPlans, Model3DService models3d,
                                   ElementTypeRepository elementTypes, MaterialRepository materials,
                                   AiServiceClient aiService, ActivityLogService activity,
                                   PlatformTransactionManager transactions) {
        this.analysisService = analysisService;
        this.analyses = analyses;
        this.floorPlans = floorPlans;
        this.models3d = models3d;
        this.elementTypes = elementTypes;
        this.materials = materials;
        this.aiService = aiService;
        this.activity = activity;
        this.tx = new TransactionTemplate(transactions);
    }

    /** Crea el análisis y corre el ciclo completo en una sola petición. */
    public AnalysisResponse createAndRun(CurrentUser user, Long floorPlanId, Long aiModelId, String scale) {
        AnalysisResponse created = analysisService.create(user, floorPlanId, aiModelId);
        return run(user, created.id(), scale);
    }

    /**
     * Corre el ciclo completo de un análisis en PENDING (o reintenta uno en FAILED).
     * Responde el análisis ya terminado: COMPLETED con sus elementos y su modelo 3D,
     * o FAILED con el motivo en errorMessage.
     */
    public AnalysisResponse run(CurrentUser user, Long analysisId, String scale) {
        long startedAt = System.currentTimeMillis();

        // 1. Validar permisos y pasar a PROCESSING (transacción corta).
        Job job = Objects.requireNonNull(tx.execute(status -> start(user, analysisId, scale)));

        // 2. Llamar al servicio de IA (sin transacción).
        AiServiceResponse result;
        try {
            result = aiService.analyze(job.imageUrl(), job.scale(), job.provider(), job.modelIdentifier());
        } catch (RuntimeException e) {
            return fail(user, job, startedAt, e);
        }

        // 3. Guardar elementos, modelo 3D y resultado (transacción corta).
        try {
            return tx.execute(status -> complete(user, job, result, startedAt));
        } catch (RuntimeException e) {
            return fail(user, job, startedAt, e);
        }
    }

    private Job start(CurrentUser user, Long analysisId, String scale) {
        AiAnalysis analysis = analysisService.findManageable(user, analysisId);
        if (analysis.getStatus() == AnalysisStatus.FAILED) {
            move(analysis, AnalysisStatus.PENDING);          // reintento
            analysis.setErrorMessage(null);
        }
        if (analysis.getStatus() != AnalysisStatus.PENDING) {
            throw ApiException.conflict("El análisis ya está en " + analysis.getStatus()
                    + "; solo se puede correr uno en PENDING o reintentar uno en FAILED.");
        }
        FloorPlan plan = floorPlans.findManageable(user, analysis.getFloorPlanId());
        AiModel model = analysisService.requireActiveModel(analysis.getAiModelId());
        move(analysis, AnalysisStatus.PROCESSING);
        analyses.save(analysis);

        String usedScale = scale == null || scale.isBlank() ? plan.getScale() : scale.trim();
        return new Job(analysis.getId(), plan.getId(), plan.getFloorLevel(), model.getName(),
                floorPlans.imageUrl(plan), usedScale, model.getProvider(), model.getModelIdentifier());
    }

    private AnalysisResponse complete(CurrentUser user, Job job, AiServiceResponse result, long startedAt) {
        List<ElementInput> rows = toRows(result.elements(), names(elementTypes.findAll()), names(materials.findAll()));
        analysisService.saveElements(user, job.analysisId(), rows);
        models3d.create(user, new Model3DRequest(job.floorPlanId(), job.analysisId(), null, "GLB", rows.size()));

        AiAnalysis analysis = analyses.findById(job.analysisId())
                .orElseThrow(() -> ApiException.notFound("El análisis ya no existe."));
        move(analysis, AnalysisStatus.COMPLETED);
        analysis.setDurationMs(elapsed(startedAt));
        analysis.setRawResponse(summaryJson(result, job));
        AnalysisResponse response = AnalysisResponse.from(analyses.save(analysis));

        activity.record(user.id(), "Análisis completado", job.modelName() + " detectó " + rows.size()
                + " elementos en el plano del piso " + job.floorLevel() + ".");
        return response;
    }

    private AnalysisResponse fail(CurrentUser user, Job job, long startedAt, RuntimeException error) {
        String message = publicMessage(error);
        log.warn("El análisis {} falló: {}", job.analysisId(), error.toString());
        AnalysisResponse response = tx.execute(status -> {
            AiAnalysis analysis = analyses.findById(job.analysisId())
                    .orElseThrow(() -> ApiException.notFound("El análisis ya no existe."));
            if (analysis.getStatus().canMoveTo(AnalysisStatus.FAILED)) {
                analysis.setStatus(AnalysisStatus.FAILED);
            }
            analysis.setErrorMessage(message);
            analysis.setDurationMs(elapsed(startedAt));
            return AnalysisResponse.from(analyses.save(analysis));
        });
        activity.record(user.id(), "Análisis fallido", job.modelName() + " no pudo analizar el plano del piso "
                + job.floorLevel() + ": " + message);
        return response;
    }

    /** Traduce los nombres que devuelve la IA ("Muro", "Ladrillo") a los ids de los catálogos. */
    static List<ElementInput> toRows(List<DetectedElement> detected, Map<String, Long> typeIds,
                                     Map<String, Long> materialIds) {
        if (detected == null) {
            return List.of();
        }
        return detected.stream()
                .filter(Objects::nonNull)
                .filter(e -> e.type() != null && typeIds.containsKey(e.type()))
                .filter(e -> e.startX() != null && e.startY() != null)
                .map(e -> new ElementInput(
                        typeIds.get(e.type()),
                        e.material() == null ? null : materialIds.get(e.material()),
                        e.startX(), e.startY(), e.endX(), e.endY(),
                        e.width(), e.height(), e.thickness(),
                        e.confidence() == null ? null : Math.min(1, Math.max(0, e.confidence()))))
                .toList();
    }

    /** Solo los mensajes pensados para el usuario (ApiException); el resto queda en el log. */
    static String publicMessage(RuntimeException error) {
        String message = error instanceof ApiException && error.getMessage() != null
                ? error.getMessage()
                : "El servicio de IA no pudo analizar el plano. Intenta de nuevo en un momento.";
        return message.length() > MAX_ERROR_CHARS ? message.substring(0, MAX_ERROR_CHARS) : message;
    }

    private static void move(AiAnalysis analysis, AnalysisStatus target) {
        if (!analysis.getStatus().canMoveTo(target)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Un análisis en " + analysis.getStatus() + " no puede pasar a " + target + ".");
        }
        analysis.setStatus(target);
    }

    private static String summaryJson(AiServiceResponse result, Job job) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (result.summary() != null) {
            summary.putAll(result.summary());
        }
        summary.remove("validation"); // la firma profesional solo la escribe ReviewService
        summary.put("analyzer", job.modelName());
        summary.put("provider", result.provider() == null ? job.provider() : result.provider());
        summary.put("service", "renderia-ai");
        summary.put("serviceDurationMs", result.durationMs());
        summary.put("orchestratedBy", "backend");
        return JSON.writeValueAsString(summary);
    }

    private static Map<String, Long> names(List<? extends CatalogEntity> rows) {
        Map<String, Long> ids = new HashMap<>();
        rows.forEach(row -> ids.put(row.getName(), row.getId()));
        return ids;
    }

    private static int elapsed(long startedAt) {
        return (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startedAt);
    }

    /** Datos que se leen en la primera transacción y se usan fuera de ella. */
    record Job(Long analysisId, Long floorPlanId, int floorLevel, String modelName,
               String imageUrl, String scale, String provider, String modelIdentifier) {
    }
}
