package com.renderia.renderia_backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.dto.AiModelResponse;
import com.renderia.renderia_backend.dto.AiModelUpdateRequest;
import com.renderia.renderia_backend.service.AiModelService;

@RestController
@RequestMapping("/api/v1/ai-models")
public class AiModelController {

    private final AiModelService service;

    public AiModelController(AiModelService service) {
        this.service = service;
    }

    @GetMapping
    public List<AiModelResponse> list() {
        return service.list();
    }

    @PutMapping("/{id}")
    public AiModelResponse update(@PathVariable Long id, @RequestBody AiModelUpdateRequest request) {
        return service.update(id, request);
    }
}
