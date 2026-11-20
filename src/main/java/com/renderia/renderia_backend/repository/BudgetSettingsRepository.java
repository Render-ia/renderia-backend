package com.renderia.renderia_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.renderia.renderia_backend.model.BudgetSettings;

public interface BudgetSettingsRepository extends JpaRepository<BudgetSettings, Long> {
}
