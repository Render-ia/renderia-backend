package com.renderia.renderia_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.renderia.renderia_backend.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByOrderByCreatedAtDesc();

    List<Project> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    /** Projects the user owns or that were shared with them, newest first. */
    @Query("""
            SELECT p FROM Project p
            WHERE p.userId = :userId
               OR p.id IN (SELECT m.projectId FROM ProjectMember m WHERE m.userId = :userId)
            ORDER BY p.createdAt DESC
            """)
    List<Project> findAccessible(@Param("userId") Long userId);
}
