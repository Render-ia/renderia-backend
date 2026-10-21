package com.renderia.renderia_backend.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.dto.AiAnalyzeRequest;
import com.renderia.renderia_backend.security.CurrentUser;
import com.renderia.renderia_backend.service.AiAnalyzeService;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiAnalyzeService service;

    public AiController(AiAnalyzeService service) {
        this.service = service;
    }

    /** Analiza un plano con el modelo elegido y devuelve { elements, summary }. */
    @PostMapping("/analyze")
    public Map<String, Object> analyze(@AuthenticationPrincipal Jwt jwt, @RequestBody AiAnalyzeRequest request) {
        return service.analyze(CurrentUser.from(jwt), request);
    }
}
