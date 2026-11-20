package com.renderia.renderia_backend.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renderia.renderia_backend.validation.AnalysisSummary;
import com.renderia.renderia_backend.validation.ElementMeasures;
import com.renderia.renderia_backend.validation.InputValidator;
import com.renderia.renderia_backend.dto.AnalysisResponse;
import com.renderia.renderia_backend.dto.AnalysisUpdateRequest;
import com.renderia.renderia_backend.dto.ElementInput;
import com.renderia.renderia_backend.dto.ElementResponse;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.AiAnalysis;
import com.renderia.renderia_backend.model.AiModel;
import com.renderia.renderia_backend.model.AnalysisStatus;
import com.renderia.renderia_backend.model.CatalogEntity;
import com.renderia.renderia_backend.model.StructuralElement;
import com.renderia.renderia_backend.repository.AiAnalysisRepository;
import com.renderia.renderia_backend.repository.AiModelRepository;
import com.renderia.renderia_backend.repository.ElementTypeRepository;
import com.renderia.renderia_backend.repository.MaterialRepository;
import com.renderia.renderia_backend.repository.Model3DRepository;
import com.renderia.renderia_backend.repository.StructuralElementRepository;
import com.renderia.renderia_backend.security.CurrentUser;

/** Análisis de IA y los elementos que encontraron, con los permisos del proyecto del plano. */
@Service
public class AnalysisService {

    /** A real plan gives a few hundred elements; this keeps one analysis from filling the database. */
    static final int MAX_ELEMENTS_PER_ANALYSIS = 2000;
    /** Coordinates are meters on the plan; 100 km is far beyond any building. */
    static final double MAX_COORDINATE = 100_000;

    private final AiAnalysisRepository analyses;
    private final StructuralElementRepository elements;
    private final AiModelRepository aiModels;
    private final ElementTypeRepository elementTypes;
    private final MaterialRepository materials;
    private final FloorPlanService floorPlans;
    private final Model3DRepository models3d;

    public AnalysisService(AiAnalysisRepository analyses, StructuralElementRepository elements,
                           AiModelRepository aiModels, ElementTypeRepository elementTypes,
                           MaterialRepository materials, FloorPlanService floorPlans,
                           Model3DRepository models3d) {
        this.analyses = analyses;
        this.elements = elements;
        this.aiModels = aiModels;
        this.elementTypes = elementTypes;
        this.materials = materials;
        this.floorPlans = floorPlans;
        this.models3d = models3d;
    }

    @Transactional(readOnly = true)
    public List<AnalysisResponse> list(CurrentUser user) {
        List<AiAnalysis> result = user.policy().canViewAllProjects()
                ? analyses.findAllByOrderByCreatedAtDesc()
                : analyses.findAllOfUser(user.id());
        return result.stream().map(AnalysisResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AnalysisResponse get(CurrentUser user, Long id) {
        return AnalysisResponse.from(findVisible(user, id));
    }

    /** Crea el análisis en PENDING para un plano que el usuario puede analizar. */
    @Transactional
    public AnalysisResponse create(CurrentUser user, Long floorPlanId, Long aiModelId) {
        if (floorPlanId == null || aiModelId == null) {
            throw ApiException.badRequest("Elige el plano y el modelo de IA.");
        }
        floorPlans.findManageable(user, floorPlanId);
        requireActiveModel(aiModelId);

        AiAnalysis analysis = new AiAnalysis();
        analysis.setFloorPlanId(floorPlanId);
        analysis.setAiModelId(aiModelId);
        return AnalysisResponse.from(analyses.save(analysis));
    }

    /** Cambia el estado (solo por las transiciones permitidas) y guarda los datos del resultado. */
    @Transactional
    public AnalysisResponse update(CurrentUser user, Long id, AnalysisUpdateRequest request) {
        AiAnalysis analysis = findManageable(user, id);
        if (request.status() != null) {
            AnalysisStatus target = parseStatus(request.status());
            if (target != analysis.getStatus()) {
                if (!analysis.getStatus().canMoveTo(target)) {
                    throw ApiException.badRequest("Un análisis en " + analysis.getStatus()
                            + " no puede pasar a " + target + ".");
                }
                analysis.setStatus(target);
            }
        }
        if (request.rawResponse() != null) {
            analysis.setRawResponse(AnalysisSummary.fromClient(request.rawResponse(), analysis.getRawResponse()));
        }
        if (request.errorMessage() != null) {
            analysis.setErrorMessage(InputValidator.optionalText(request.errorMessage(), "El mensaje de error", 2000));
        }
        if (request.durationMs() != null) {
            analysis.setDurationMs(Math.max(0, request.durationMs()));
        }
        return AnalysisResponse.from(analyses.save(analysis));
    }

    @Transactional
    public void delete(CurrentUser user, Long id) {
        AiAnalysis analysis = findManageable(user, id);
        // El modelo 3D apunta al análisis: se borra primero para no romper la llave foránea.
        models3d.deleteAllByAnalysisId(analysis.getId());
        analyses.delete(analysis);
    }

    @Transactional(readOnly = true)
    public List<ElementResponse> elements(CurrentUser user, Long analysisId) {
        findVisible(user, analysisId);
        return elements.findAllByAiAnalysisIdOrderByIdAsc(analysisId).stream().map(ElementResponse::from).toList();
    }

    @Transactional
    public List<ElementResponse> saveElements(CurrentUser user, Long analysisId, List<ElementInput> inputs) {
        findManageable(user, analysisId);
        if (inputs == null) {
            throw ApiException.badRequest("Envía la lista de elementos.");
        }
        if (elements.countByAiAnalysisId(analysisId) + inputs.size() > MAX_ELEMENTS_PER_ANALYSIS) {
            throw ApiException.badRequest("Un análisis admite máximo " + MAX_ELEMENTS_PER_ANALYSIS + " elementos.");
        }
        Set<Long> typeIds = ids(elementTypes.findAll());
        Set<Long> materialIds = ids(materials.findAll());

        List<StructuralElement> rows = inputs.stream().map(input -> {
            if (input == null) {
                throw ApiException.badRequest("Hay un elemento vacío en la lista.");
            }
            if (input.elementTypeId() == null || !typeIds.contains(input.elementTypeId())) {
                throw ApiException.badRequest("Hay un elemento con un tipo que no existe.");
            }
            if (input.materialId() != null && !materialIds.contains(input.materialId())) {
                throw ApiException.badRequest("Hay un elemento con un material que no existe.");
            }
            if (input.startX() == null || input.startY() == null) {
                throw ApiException.badRequest("Cada elemento necesita startX y startY.");
            }
            StructuralElement e = new StructuralElement();
            e.setAiAnalysisId(analysisId);
            e.setElementTypeId(input.elementTypeId());
            e.setMaterialId(input.materialId());
            e.setStartX(coordinate(input.startX()));
            e.setStartY(coordinate(input.startY()));
            e.setEndX(coordinate(input.endX()));
            e.setEndY(coordinate(input.endY()));
            e.setWidth(detectedMeasure(input.width(), "El ancho"));
            e.setHeight(detectedMeasure(input.height(), "La altura"));
            e.setThickness(detectedMeasure(input.thickness(), "El espesor"));
            e.setConfidence(input.confidence() == null ? null : Math.min(1, Math.max(0, input.confidence())));
            return e;
        }).toList();

        return elements.saveAll(rows).stream().map(ElementResponse::from).toList();
    }

    /**
     * Cambia el tipo, el material o las medidas (height, thickness, width) de un elemento.
     * Enviar "materialId": null lo deja sin material.
     */
    @Transactional
    public ElementResponse updateElement(CurrentUser user, Long elementId, Map<String, Object> changes) {
        StructuralElement element = elements.findById(elementId)
                .orElseThrow(() -> ApiException.notFound("El elemento no existe."));
        findManageable(user, element.getAiAnalysisId());

        if (changes.get("elementTypeId") != null) {
            Long typeId = toLong(changes.get("elementTypeId"));
            if (!elementTypes.existsById(typeId)) {
                throw ApiException.badRequest("El tipo de elemento no existe.");
            }
            element.setElementTypeId(typeId);
        }
        if (changes.containsKey("materialId")) {
            Long materialId = changes.get("materialId") == null ? null : toLong(changes.get("materialId"));
            if (materialId != null && !materials.existsById(materialId)) {
                throw ApiException.badRequest("El material no existe.");
            }
            element.setMaterialId(materialId);
        }
        ElementMeasures.apply(element, changes);
        return ElementResponse.from(elements.save(element));
    }

    @Transactional
    public void removeElement(CurrentUser user, Long elementId) {
        StructuralElement element = elements.findById(elementId)
                .orElseThrow(() -> ApiException.notFound("El elemento no existe."));
        findManageable(user, element.getAiAnalysisId());
        elements.delete(element);
    }

    public AiAnalysis findVisible(CurrentUser user, Long id) {
        AiAnalysis analysis = analyses.findById(id)
                .orElseThrow(() -> ApiException.notFound("El análisis no existe o no tienes acceso a él."));
        floorPlans.findVisible(user, analysis.getFloorPlanId());
        return analysis;
    }

    public AiAnalysis findManageable(CurrentUser user, Long id) {
        AiAnalysis analysis = findVisible(user, id);
        floorPlans.findManageable(user, analysis.getFloorPlanId());
        return analysis;
    }

    public AiModel requireActiveModel(Long aiModelId) {
        AiModel model = aiModels.findById(aiModelId)
                .orElseThrow(() -> ApiException.badRequest("El modelo de IA no existe."));
        if (!model.isActive()) {
            throw ApiException.badRequest("El modelo de IA \"" + model.getName() + "\" está desactivado.");
        }
        return model;
    }

    static Double coordinate(Double value) {
        if (value == null) {
            return null;
        }
        if (!Double.isFinite(value) || Math.abs(value) > MAX_COORDINATE) {
            throw ApiException.badRequest("Hay un elemento con coordenadas fuera del plano.");
        }
        return value;
    }

    /** A detected measure of 0 means "unknown"; negative or absurd values are rejected. */
    static Double detectedMeasure(Double value, String label) {
        if (value == null || value == 0) {
            return null;
        }
        if (value > 0 && value < ElementMeasures.MIN_METERS) {
            return ElementMeasures.MIN_METERS;
        }
        return ElementMeasures.parse(value, label);
    }

    private static AnalysisStatus parseStatus(String value) {
        try {
            return AnalysisStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Estado de análisis no válido: " + value + ".");
        }
    }

    private static Set<Long> ids(List<? extends CatalogEntity> rows) {
        Set<Long> ids = new HashSet<>();
        rows.forEach(row -> ids.add(row.getId()));
        return ids;
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Id no válido: " + value + ".");
        }
    }
}
