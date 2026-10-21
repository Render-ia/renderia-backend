package com.renderia.renderia_backend.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.renderia.renderia_backend.model.FloorPlan;

public interface FloorPlanRepository extends JpaRepository<FloorPlan, Long> {

    List<FloorPlan> findAllByOrderByUploadedAtDesc();

    List<FloorPlan> findAllByProjectIdInOrderByUploadedAtDesc(Collection<Long> projectIds);

    List<FloorPlan> findAllByProjectIdOrderByFloorLevelAsc(Long projectId);
}
