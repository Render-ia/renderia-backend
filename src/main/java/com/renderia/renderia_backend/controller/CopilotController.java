package com.renderia.renderia_backend.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.dto.CopilotRequest;
import com.renderia.renderia_backend.dto.RenderRequest;
import com.renderia.renderia_backend.security.CurrentUser;
import com.renderia.renderia_backend.service.CopilotService;

/** Engineering copilot and AI render of one analysis. Both need a session and count against a daily quota. */
@RestController
@RequestMapping("/api/v1")
public class CopilotController {

    private final CopilotService service;

    public CopilotController(CopilotService service) {
        this.service = service;
    }

    @PostMapping("/analyses/{id}/copilot")
    public Map<String, Object> ask(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @RequestBody CopilotRequest request) {
        return service.ask(CurrentUser.from(jwt), id, request);
    }

    @PostMapping("/analyses/{id}/render")
    public Map<String, Object> render(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @RequestBody RenderRequest request) {
        return service.render(CurrentUser.from(jwt), id, request);
    }

    @GetMapping("/ai/capabilities")
    public Map<String, Object> capabilities(@AuthenticationPrincipal Jwt jwt) {
        return service.capabilities(CurrentUser.from(jwt));
    }
}
