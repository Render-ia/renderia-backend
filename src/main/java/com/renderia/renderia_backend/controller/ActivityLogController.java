package com.renderia.renderia_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.dto.ActivityRequest;
import com.renderia.renderia_backend.dto.ActivityResponse;
import com.renderia.renderia_backend.security.CurrentUser;
import com.renderia.renderia_backend.service.ActivityLogService;

@RestController
@RequestMapping("/api/v1/activity-logs")
public class ActivityLogController {

    private final ActivityLogService service;

    public ActivityLogController(ActivityLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<ActivityResponse> recent(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam(required = false) Integer limit,
                                         @RequestParam(required = false) Long userId) {
        return service.recent(CurrentUser.from(jwt), limit, userId);
    }

    @PostMapping
    public ResponseEntity<ActivityResponse> create(@AuthenticationPrincipal Jwt jwt, @RequestBody ActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(CurrentUser.from(jwt), request));
    }
}
