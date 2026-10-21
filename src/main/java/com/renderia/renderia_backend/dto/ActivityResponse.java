package com.renderia.renderia_backend.dto;

import java.time.Instant;

import com.renderia.renderia_backend.model.ActivityLog;

public record ActivityResponse(Long id, Long userId, String action, String details, Instant createdAt) {

    public static ActivityResponse from(ActivityLog log) {
        return new ActivityResponse(log.getId(), log.getUserId(), log.getAction(),
                log.getDetails() == null ? "" : log.getDetails(), log.getCreatedAt());
    }
}
