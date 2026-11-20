package com.renderia.renderia_backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.dto.ReviewResponse;
import com.renderia.renderia_backend.security.CurrentUser;
import com.renderia.renderia_backend.service.ReviewService;

@RestController
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/analyses/{id}/review")
    public ReviewResponse review(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.review(CurrentUser.from(jwt), id);
    }
}
