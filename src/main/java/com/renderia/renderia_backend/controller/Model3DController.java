package com.renderia.renderia_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.dto.Model3DRequest;
import com.renderia.renderia_backend.dto.Model3DResponse;
import com.renderia.renderia_backend.security.CurrentUser;
import com.renderia.renderia_backend.service.Model3DService;

@RestController
@RequestMapping("/api/v1")
public class Model3DController {

    private final Model3DService service;

    public Model3DController(Model3DService service) {
        this.service = service;
    }

    @GetMapping("/models-3d")
    public List<Model3DResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(CurrentUser.from(jwt));
    }

    @GetMapping("/analyses/{id}/model-3d")
    public Model3DResponse byAnalysis(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.byAnalysis(CurrentUser.from(jwt), id);
    }

    @PostMapping("/models-3d")
    public ResponseEntity<Model3DResponse> create(@AuthenticationPrincipal Jwt jwt, @RequestBody Model3DRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(CurrentUser.from(jwt), request));
    }
}
