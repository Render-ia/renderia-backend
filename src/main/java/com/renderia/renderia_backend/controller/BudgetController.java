package com.renderia.renderia_backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.security.CurrentUser;
import com.renderia.renderia_backend.service.BudgetService;

import tools.jackson.databind.JsonNode;

/** Saved budget inputs (parameters, prices and AIU) of one analysis. */
@RestController
@RequestMapping("/api/v1/analyses/{id}/budget-inputs")
public class BudgetController {

    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    @GetMapping
    public JsonNode get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(CurrentUser.from(jwt), id);
    }

    @PutMapping
    public JsonNode save(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @RequestBody JsonNode body) {
        return service.save(CurrentUser.from(jwt), id, body);
    }
}
