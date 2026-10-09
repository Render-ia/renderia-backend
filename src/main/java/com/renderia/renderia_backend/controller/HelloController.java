package com.renderia.renderia_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HelloController {

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        return Map.of(
            "proyecto", "Render.IA",
            "descripcion", "IA que convierte planos 2D en modelos estructurales 3D",
            "integrantes", List.of("Juan David Moreno", "Felipe Cerón"),
            "mensaje", "Hola mundo desde el backend"
        );
    }
}