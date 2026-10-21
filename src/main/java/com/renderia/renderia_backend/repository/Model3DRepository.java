package com.renderia.renderia_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.renderia.renderia_backend.model.Model3D;

public interface Model3DRepository extends JpaRepository<Model3D, Long> {

    List<Model3D> findAllByOrderByCreatedAtDesc();

    Optional<Model3D> findFirstByAiAnalysisIdOrderByIdDesc(Long aiAnalysisId);

    @Query("""
            SELECT m FROM Model3D m
            WHERE m.floorPlanId IN (
                SELECT f.id FROM FloorPlan f WHERE f.projectId IN (
                    SELECT p.id FROM Project p WHERE p.userId = :userId))
            ORDER BY m.createdAt DESC
            """)
    List<Model3D> findAllOfUser(@Param("userId") Long userId);
}
