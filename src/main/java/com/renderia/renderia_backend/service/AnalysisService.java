package com.renderia.renderia_backend.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.renderia.renderia_backend.repository.StructuralElementRepository;
import com.renderia.renderia_backend.security.CurrentUser;

/** Análisis de IA y los elementos que encontraron, con los permisos del proyecto del plano. */
@Service
public class AnalysisService {

    private final AiAnalysisRepository analyses;
    private final StructuralElementRepository elements;
    private final AiModelRepository aiModels;
    private final ElementTypeRepository elementTypes;
    private final MaterialRepository materials;
    private final FloorPlanService floorPlans;

    public AnalysisService(AiAnalysisRepository analyses, StructuralElementRepository elements,
                           AiModelRepository aiModels, ElementTypeRepository elementTypes,
                           MaterialRepository materials, FloorPlanService floorPlans) {
        this.analyses = analyses;
        this.elements = elements;
        this.aiModels = aiModels;
        this.elementTypes = elementTypes;
        this.materials = materials;
        this.floorPlans = floorPlans;
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
            analysis.setRawResponse(request.rawResponse());
        }
        if (request.errorMessage() != null) {
            analysis.setErrorMessage(request.errorMessage());
        }
        if (request.durationMs() != null) {
            analysis.setDurationMs(Math.max(0, request.durationMs()));
        }
        return AnalysisResponse.from(analyses.save(analysis));
    }

    @Transactional
    public void delete(CurrentUser user, Long id) {
        analyses.delete(findManageable(user, id));
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
        Set<Long> typeIds = ids(elementTypes.findAll());
        Set<Long> materialIds = ids(materials.findAll());

        List<StructuralElement> rows = inputs.stream().map(input -> {
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
            e.setStartX(input.startX());
            e.setStartY(input.startY());
            e.setEndX(input.endX());
            e.setEndY(input.endY());
            e.setWidth(input.width());
            e.setHeight(input.height());
            e.setThickness(input.thickness());
            e.setConfidence(input.confidence() == null ? null : Math.min(1, Math.max(0, input.confidence())));
            return e;
        }).toList();

        return elements.saveAll(rows).stream().map(ElementResponse::from).toList();
    }

    /** Cambia el tipo o el material de un elemento. Enviar "materialId": null lo deja sin material. */
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
