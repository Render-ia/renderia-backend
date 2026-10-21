package com.renderia.renderia_backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renderia.renderia_backend.dto.Model3DRequest;
import com.renderia.renderia_backend.dto.Model3DResponse;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.AiAnalysis;
import com.renderia.renderia_backend.model.Model3D;
import com.renderia.renderia_backend.repository.Model3DRepository;
import com.renderia.renderia_backend.security.CurrentUser;

/** Registro de los modelos 3D generados a partir de cada análisis. */
@Service
public class Model3DService {

    private final Model3DRepository models;
    private final AnalysisService analyses;

    public Model3DService(Model3DRepository models, AnalysisService analyses) {
        this.models = models;
        this.analyses = analyses;
    }

    @Transactional(readOnly = true)
    public List<Model3DResponse> list(CurrentUser user) {
        List<Model3D> result = user.policy().canViewAllProjects()
                ? models.findAllByOrderByCreatedAtDesc()
                : models.findAllOfUser(user.id());
        return result.stream().map(Model3DResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Model3DResponse byAnalysis(CurrentUser user, Long analysisId) {
        analyses.findVisible(user, analysisId);
        return models.findFirstByAiAnalysisIdOrderByIdDesc(analysisId)
                .map(Model3DResponse::from)
                .orElseThrow(() -> ApiException.notFound("Este análisis todavía no tiene modelo 3D."));
    }

    @Transactional
    public Model3DResponse create(CurrentUser user, Model3DRequest request) {
        if (request == null || request.aiAnalysisId() == null) {
            throw ApiException.badRequest("Indica el análisis del modelo 3D.");
        }
        AiAnalysis analysis = analyses.findManageable(user, request.aiAnalysisId());
        Model3D model = new Model3D();
        model.setFloorPlanId(analysis.getFloorPlanId());
        model.setAiAnalysisId(analysis.getId());
        model.setFileUrl(request.fileUrl());
        model.setFormat(request.format() == null || request.format().isBlank() ? "GLB" : request.format().trim());
        model.setElementCount(request.elementCount() == null ? 0 : Math.max(0, request.elementCount()));
        return Model3DResponse.from(models.save(model));
    }
}
