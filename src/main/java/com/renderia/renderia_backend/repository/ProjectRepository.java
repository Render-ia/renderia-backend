package com.renderia.renderia_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.renderia.renderia_backend.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByOrderByCreatedAtDesc();

    List<Project> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);
}
