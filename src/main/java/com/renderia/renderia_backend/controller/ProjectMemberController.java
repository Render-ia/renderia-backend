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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.dto.ProjectMemberRequest;
import com.renderia.renderia_backend.dto.ProjectMemberResponse;
import com.renderia.renderia_backend.security.CurrentUser;
import com.renderia.renderia_backend.service.ProjectMemberService;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService service;

    public ProjectMemberController(ProjectMemberService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProjectMemberResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long projectId) {
        return service.list(CurrentUser.from(jwt), projectId);
    }

    @PostMapping
    public ResponseEntity<ProjectMemberResponse> add(@AuthenticationPrincipal Jwt jwt, @PathVariable Long projectId,
                                                     @RequestBody ProjectMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(CurrentUser.from(jwt), projectId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long projectId,
                                       @PathVariable Long userId) {
        service.remove(CurrentUser.from(jwt), projectId, userId);
        return ResponseEntity.noContent().build();
    }
}
