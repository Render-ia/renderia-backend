package com.renderia.renderia_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.renderia.renderia_backend.dto.CatalogItemRequest;
import com.renderia.renderia_backend.dto.CatalogItemResponse;
import com.renderia.renderia_backend.model.CatalogEntity;
import com.renderia.renderia_backend.service.CatalogService;

/**
 * Rutas REST comunes de un catálogo. Cada subclase solo define su URL
 * con @RequestMapping y le pasa su servicio.
 */
public abstract class CatalogController<T extends CatalogEntity> {

    private final CatalogService<T> service;

    protected CatalogController(CatalogService<T> service) {
        this.service = service;
    }

    @GetMapping
    public List<CatalogItemResponse> list() {
        return service.list();
    }

    @PostMapping
    public ResponseEntity<CatalogItemResponse> create(@RequestBody CatalogItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public CatalogItemResponse update(@PathVariable Long id, @RequestBody CatalogItemRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
