package com.renderia.renderia_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.renderia.renderia_backend.model.AiAnalysis;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {

    List<AiAnalysis> findAllByOrderByCreatedAtDesc();

    /** Análisis de los planos de los proyectos de un usuario. */
    @Query("""
            SELECT a FROM AiAnalysis a
            WHERE a.floorPlanId IN (
                SELECT f.id FROM FloorPlan f WHERE f.projectId IN (
                    SELECT p.id FROM Project p WHERE p.userId = :userId))
            ORDER BY a.createdAt DESC
            """)
    List<AiAnalysis> findAllOfUser(@Param("userId") Long userId);
}
