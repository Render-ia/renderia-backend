package com.renderia.renderia_backend.service;

import org.springframework.stereotype.Service;

import com.renderia.renderia_backend.model.Material;
import com.renderia.renderia_backend.repository.MaterialRepository;

@Service
public class MaterialService extends CatalogService<Material> {

    public MaterialService(MaterialRepository repository) {
        super(repository);
    }

    @Override
    protected Material newEntity() {
        return new Material();
    }

    @Override
    protected String label() {
        return "material";
    }
}
