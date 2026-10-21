package com.renderia.renderia_backend.controller;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.renderia.renderia_backend.dto.FloorPlanResponse;
import com.renderia.renderia_backend.model.FloorPlanFile;
import com.renderia.renderia_backend.security.CurrentUser;
import com.renderia.renderia_backend.service.FloorPlanService;

@RestController
@RequestMapping("/api/v1")
public class FloorPlanController {

    private final FloorPlanService service;

    public FloorPlanController(FloorPlanService service) {
        this.service = service;
    }

    @GetMapping("/floor-plans")
    public List<FloorPlanResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.listVisible(CurrentUser.from(jwt));
    }

    @GetMapping("/floor-plans/{id}")
    public FloorPlanResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(CurrentUser.from(jwt), id);
    }

    @GetMapping("/projects/{projectId}/floor-plans")
    public List<FloorPlanResponse> listByProject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long projectId) {
        return service.listByProject(CurrentUser.from(jwt), projectId);
    }

    @PostMapping(path = "/projects/{projectId}/floor-plans", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FloorPlanResponse> upload(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable Long projectId,
                                                    @RequestParam("file") MultipartFile file,
                                                    @RequestParam(required = false) Integer floorLevel,
                                                    @RequestParam(required = false) String scale) {
        FloorPlanResponse plan = service.upload(CurrentUser.from(jwt), projectId, file, floorLevel, scale);
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }

    @DeleteMapping("/floor-plans/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.from(jwt), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Imagen del plano. Es pública para que funcione dentro de una etiqueta img;
     * la política de seguridad impide que un SVG ejecute código.
     */
    @GetMapping("/floor-plans/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable Long id) {
        FloorPlanFile file = service.image(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox")
                .header("X-Content-Type-Options", "nosniff")
                .body(file.getData());
    }
}
