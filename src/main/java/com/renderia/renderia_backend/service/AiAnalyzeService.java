package com.renderia.renderia_backend.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renderia.renderia_backend.ai.AiServiceClient;
import com.renderia.renderia_backend.dto.AiAnalyzeRequest;
import com.renderia.renderia_backend.dto.AiServiceResponse;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.AiModel;
import com.renderia.renderia_backend.model.FloorPlan;
import com.renderia.renderia_backend.security.CurrentUser;

/**
 * Pide el análisis de un plano al servicio de IA.
 *
 * Patrón Facade: el controlador hace una sola llamada y esta clase se encarga
 * de revisar permisos, elegir el modelo, armar la petición al servicio de IA
 * y devolver el resultado listo para guardar.
 */
@Service
public class AiAnalyzeService {

    private final FloorPlanService floorPlans;
    private final AnalysisService analyses;
    private final AiServiceClient aiService;

    public AiAnalyzeService(FloorPlanService floorPlans, AnalysisService analyses, AiServiceClient aiService) {
        this.floorPlans = floorPlans;
        this.analyses = analyses;
        this.aiService = aiService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analyze(CurrentUser user, AiAnalyzeRequest request) {
        if (request == null || request.floorPlanId() == null || request.aiModelId() == null) {
            throw ApiException.badRequest("Elige el plano y el modelo de IA.");
        }
        FloorPlan plan = floorPlans.findManageable(user, request.floorPlanId());
        AiModel model = analyses.requireActiveModel(request.aiModelId());
        String scale = request.scale() == null || request.scale().isBlank() ? plan.getScale() : request.scale().trim();

        AiServiceResponse result = aiService.analyze(plan.getFileUrl(), scale, model.getProvider(),
                model.getModelIdentifier());

        Map<String, Object> summary = new LinkedHashMap<>();
        if (result.summary() != null) {
            summary.putAll(result.summary());
        }
        summary.put("service", "renderia-ai");
        summary.put("serviceDurationMs", result.durationMs());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("elements", result.elements());
        body.put("summary", summary);
        return body;
    }
}
