package com.renderia.renderia_backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.model.Material;
import com.renderia.renderia_backend.service.MaterialService;

@RestController
@RequestMapping("/api/v1/materials")
public class MaterialController extends CatalogController<Material> {

    public MaterialController(MaterialService service) {
        super(service);
    }
}
