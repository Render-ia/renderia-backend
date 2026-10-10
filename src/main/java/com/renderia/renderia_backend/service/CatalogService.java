package com.renderia.renderia_backend.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.renderia.renderia_backend.dto.CatalogItemRequest;
import com.renderia.renderia_backend.dto.CatalogItemResponse;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.CatalogEntity;

/**
 * Lógica común de los catálogos: listar, crear, editar y borrar.
 *
 * Patrón Template Method: los pasos son siempre los mismos y cada catálogo
 * concreto solo completa lo que cambia (cómo crear la entidad y cómo se llama).
 */
public abstract class CatalogService<T extends CatalogEntity> {

    private final JpaRepository<T, Long> repository;

    protected CatalogService(JpaRepository<T, Long> repository) {
        this.repository = repository;
    }

    /** Crea una entidad vacía del catálogo concreto. */
    protected abstract T newEntity();

    /** Nombre del catálogo en singular, para los mensajes de error. */
    protected abstract String label();

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> list() {
        return repository.findAll(Sort.by("id")).stream()
                .map(CatalogItemResponse::from)
                .toList();
    }

    @Transactional
    public CatalogItemResponse create(CatalogItemRequest request) {
        T entity = newEntity();
        apply(entity, request);
        return CatalogItemResponse.from(save(entity));
    }

    @Transactional
    public CatalogItemResponse update(Long id, CatalogItemRequest request) {
        T entity = findOrFail(id);
        apply(entity, request);
        return CatalogItemResponse.from(save(entity));
    }

    @Transactional
    public void delete(Long id) {
        T entity = findOrFail(id);
        try {
            repository.delete(entity);
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("No se puede borrar: este " + label() + " está en uso.");
        }
    }

    private T findOrFail(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No existe el " + label() + " con id " + id + "."));
    }

    private void apply(T entity, CatalogItemRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw ApiException.badRequest("El nombre es obligatorio.");
        }
        entity.setName(request.name().trim());
        entity.setDescription(request.description() == null ? null : request.description().trim());
    }

    private T save(T entity) {
        try {
            return repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("Ya existe un " + label() + " con ese nombre.");
        }
    }
}
