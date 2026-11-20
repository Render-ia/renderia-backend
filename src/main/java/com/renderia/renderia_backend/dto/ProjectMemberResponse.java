package com.renderia.renderia_backend.dto;

import java.time.Instant;

import com.renderia.renderia_backend.model.ProjectMember;
import com.renderia.renderia_backend.model.User;

/** A user a project is shared with. Only public data, never the password hash. */
public record ProjectMemberResponse(Long userId, String fullName, String email, String role, Instant sharedAt) {

    public static ProjectMemberResponse from(ProjectMember member, User user) {
        return new ProjectMemberResponse(user.getId(), user.getFullName(), user.getEmail(),
                user.getRole().getName(), member.getCreatedAt());
    }
}
