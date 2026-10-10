package com.renderia.renderia_backend.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renderia.renderia_backend.dto.AiModelResponse;
import com.renderia.renderia_backend.dto.AiModelUpdateRequest;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.AiModel;
import com.renderia.renderia_backend.repository.AiModelRepository;

/**
 * Activa o desactiva modelos de IA y elige el predeterminado.
 * Reglas: solo un modelo puede ser predeterminado, y el predeterminado debe estar activo.
 */
@Service
public class AiModelService {

    private final AiModelRepository repository;

    public AiModelService(AiModelRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AiModelResponse> list() {
        return repository.findAll(Sort.by("id")).stream()
                .map(AiModelResponse::from)
                .toList();
    }

    @Transactional
    public AiModelResponse update(Long id, AiModelUpdateRequest request) {
        AiModel model = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No existe el modelo de IA con id " + id + "."));

        if (request.isActive() != null) {
            if (!request.isActive() && model.isDefaultModel()) {
                throw ApiException.badRequest("No se puede desactivar el modelo predeterminado. Elige otro primero.");
            }
            model.setActive(request.isActive());
        }

        if (Boolean.TRUE.equals(request.isDefault())) {
            if (!model.isActive()) {
                throw ApiException.badRequest("Solo un modelo activo puede ser el predeterminado.");
            }
            repository.findAll().forEach(other -> other.setDefaultModel(other.getId().equals(model.getId())));
        } else if (Boolean.FALSE.equals(request.isDefault()) && model.isDefaultModel()) {
            throw ApiException.badRequest("Debe haber un modelo predeterminado. Marca otro en su lugar.");
        }

        return AiModelResponse.from(repository.save(model));
    }
}
