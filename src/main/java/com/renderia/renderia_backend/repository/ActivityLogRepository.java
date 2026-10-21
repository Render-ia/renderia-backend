package com.renderia.renderia_backend.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.renderia.renderia_backend.model.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable page);

    List<ActivityLog> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable page);
}
