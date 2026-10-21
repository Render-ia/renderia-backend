package com.renderia.renderia_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.renderia.renderia_backend.model.StructuralElement;

public interface StructuralElementRepository extends JpaRepository<StructuralElement, Long> {

    List<StructuralElement> findAllByAiAnalysisIdOrderByIdAsc(Long aiAnalysisId);
}
