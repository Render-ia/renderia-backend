package com.renderia.renderia_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.dto.AnalysisCreateRequest;
import com.renderia.renderia_backend.dto.AnalysisResponse;
import com.renderia.renderia_backend.dto.AnalysisRunRequest;
import com.renderia.renderia_backend.dto.AnalysisUpdateRequest;
import com.renderia.renderia_backend.dto.ElementInput;
import com.renderia.renderia_backend.dto.ElementResponse;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.security.CurrentUser;
import com.renderia.renderia_backend.service.AnalysisPipelineService;
import com.renderia.renderia_backend.service.AnalysisService;

@RestController
@RequestMapping("/api/v1")
public class AnalysisController {

    private final AnalysisService service;
    private final AnalysisPipelineService pipeline;

    public AnalysisController(AnalysisService service, AnalysisPipelineService pipeline) {
        this.service = service;
        this.pipeline = pipeline;
    }

    @GetMapping("/analyses")
    public List<AnalysisResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(CurrentUser.from(jwt));
    }

    @GetMapping("/analyses/{id}")
    public AnalysisResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(CurrentUser.from(jwt), id);
    }

    @PostMapping("/analyses")
    public ResponseEntity<AnalysisResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestBody AnalysisCreateRequest request) {
        AnalysisResponse analysis = service.create(CurrentUser.from(jwt), request.floorPlanId(), request.aiModelId());
        return ResponseEntity.status(HttpStatus.CREATED).body(analysis);
    }

    /**
     * Ciclo completo en el backend: crea el análisis, llama al servicio de IA, guarda los
     * elementos y el modelo 3D y deja el análisis en COMPLETED (o FAILED con el motivo).
     */
    @PostMapping("/analyses/run")
    public ResponseEntity<AnalysisResponse> createAndRun(@AuthenticationPrincipal Jwt jwt,
                                                         @RequestBody AnalysisRunRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Elige el plano y el modelo de IA.");
        }
        AnalysisResponse analysis = pipeline.createAndRun(CurrentUser.from(jwt), request.floorPlanId(),
                request.aiModelId(), request.scale());
        return ResponseEntity.status(HttpStatus.CREATED).body(analysis);
    }

    /** Corre (o reintenta) el ciclo completo de un análisis que ya existe. */
    @PostMapping("/analyses/{id}/run")
    public AnalysisResponse run(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                @RequestBody(required = false) AnalysisRunRequest request) {
        return pipeline.run(CurrentUser.from(jwt), id, request == null ? null : request.scale());
    }

    @PutMapping("/analyses/{id}")
    public AnalysisResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                   @RequestBody AnalysisUpdateRequest request) {
        return service.update(CurrentUser.from(jwt), id, request);
    }

    @DeleteMapping("/analyses/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.from(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/analyses/{id}/elements")
    public List<ElementResponse> elements(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.elements(CurrentUser.from(jwt), id);
    }

    @PostMapping("/analyses/{id}/elements")
    public ResponseEntity<List<ElementResponse>> saveElements(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                              @RequestBody List<ElementInput> elements) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveElements(CurrentUser.from(jwt), id, elements));
    }

    @PutMapping("/elements/{id}")
    public ElementResponse updateElement(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                         @RequestBody Map<String, Object> changes) {
        return service.updateElement(CurrentUser.from(jwt), id, changes);
    }

    @DeleteMapping("/elements/{id}")
    public ResponseEntity<Void> removeElement(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.removeElement(CurrentUser.from(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
