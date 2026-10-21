package com.renderia.renderia_backend.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.dto.ProjectRequest;
import com.renderia.renderia_backend.dto.ProjectResponse;
import com.renderia.renderia_backend.security.CurrentUser;
import com.renderia.renderia_backend.service.ProjectService;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProjectResponse> list(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam(required = false) Long userId) {
        return service.list(CurrentUser.from(jwt), userId);
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(CurrentUser.from(jwt), id);
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                  @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(CurrentUser.from(jwt), request));
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @RequestBody ProjectRequest request) {
        return service.update(CurrentUser.from(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.from(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
