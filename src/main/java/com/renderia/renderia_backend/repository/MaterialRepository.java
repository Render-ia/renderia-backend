package com.renderia.renderia_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.renderia.renderia_backend.model.Material;

public interface MaterialRepository extends JpaRepository<Material, Long> {
}
