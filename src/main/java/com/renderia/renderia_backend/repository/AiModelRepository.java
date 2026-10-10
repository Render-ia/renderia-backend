package com.renderia.renderia_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.renderia.renderia_backend.model.AiModel;

public interface AiModelRepository extends JpaRepository<AiModel, Long> {

    Optional<AiModel> findFirstByDefaultModelTrue();
}
