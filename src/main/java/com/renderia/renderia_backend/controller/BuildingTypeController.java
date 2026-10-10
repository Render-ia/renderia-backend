package com.renderia.renderia_backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.model.BuildingType;
import com.renderia.renderia_backend.service.BuildingTypeService;

@RestController
@RequestMapping("/api/v1/building-types")
public class BuildingTypeController extends CatalogController<BuildingType> {

    public BuildingTypeController(BuildingTypeService service) {
        super(service);
    }
}
