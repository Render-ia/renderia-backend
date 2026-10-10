package com.renderia.renderia_backend.service;

import org.springframework.stereotype.Service;

import com.renderia.renderia_backend.model.ElementType;
import com.renderia.renderia_backend.repository.ElementTypeRepository;

@Service
public class ElementTypeService extends CatalogService<ElementType> {

    public ElementTypeService(ElementTypeRepository repository) {
        super(repository);
    }

    @Override
    protected ElementType newEntity() {
        return new ElementType();
    }

    @Override
    protected String label() {
        return "tipo de elemento";
    }
}
