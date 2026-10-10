package com.renderia.renderia_backend.service;

import org.springframework.stereotype.Service;

import com.renderia.renderia_backend.model.BuildingType;
import com.renderia.renderia_backend.repository.BuildingTypeRepository;

@Service
public class BuildingTypeService extends CatalogService<BuildingType> {

    public BuildingTypeService(BuildingTypeRepository repository) {
        super(repository);
    }

    @Override
    protected BuildingType newEntity() {
        return new BuildingType();
    }

    @Override
    protected String label() {
        return "tipo de edificación";
    }
}
