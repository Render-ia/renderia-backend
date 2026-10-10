package com.renderia.renderia_backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renderia.renderia_backend.model.ElementType;
import com.renderia.renderia_backend.service.ElementTypeService;

@RestController
@RequestMapping("/api/v1/element-types")
public class ElementTypeController extends CatalogController<ElementType> {

    public ElementTypeController(ElementTypeService service) {
        super(service);
    }
}
