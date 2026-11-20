package com.renderia.renderia_backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renderia.renderia_backend.dto.ProjectMemberRequest;
import com.renderia.renderia_backend.dto.ProjectMemberResponse;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.Project;
import com.renderia.renderia_backend.model.ProjectMember;
import com.renderia.renderia_backend.model.User;
import com.renderia.renderia_backend.repository.ProjectMemberRepository;
import com.renderia.renderia_backend.repository.UserRepository;
import com.renderia.renderia_backend.security.CurrentUser;
import com.renderia.renderia_backend.validation.InputValidator;

/** Sharing a project with other users (clients, reviewers, colleagues). Members get read access. */
@Service
public class ProjectMemberService {

    private final ProjectService projects;
    private final ProjectMemberRepository members;
    private final UserRepository users;

    public ProjectMemberService(ProjectService projects, ProjectMemberRepository members, UserRepository users) {
        this.projects = projects;
        this.members = members;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> list(CurrentUser user, Long projectId) {
        Project project = projects.findVisible(user, projectId);
        return members.findAllByProjectIdOrderByCreatedAtAsc(project.getId()).stream()
                .flatMap(member -> users.findById(member.getUserId())
                        .map(found -> ProjectMemberResponse.from(member, found)).stream())
                .toList();
    }

    @Transactional
    public ProjectMemberResponse add(CurrentUser user, Long projectId, ProjectMemberRequest request) {
        Project project = projects.findManageable(user, projectId);
        String email = InputValidator.requiredText(request == null ? null : request.email(), "El correo", 150)
                .toLowerCase();
        // Same message whether the email is unknown or inactive, so accounts cannot be probed.
        User target = users.findByEmailIgnoreCase(email)
                .filter(User::isActive)
                .orElseThrow(() -> ApiException.notFound("No hay una cuenta activa con ese correo."));
        if (target.getId().equals(project.getUserId())) {
            throw ApiException.badRequest("El proyecto ya es de esa persona.");
        }
        if (members.existsByProjectIdAndUserId(project.getId(), target.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "El proyecto ya está compartido con esa persona.");
        }
        ProjectMember member = members.save(new ProjectMember(project.getId(), target.getId()));
        return ProjectMemberResponse.from(member, target);
    }

    @Transactional
    public void remove(CurrentUser user, Long projectId, Long userId) {
        Project project = projects.findManageable(user, projectId);
        ProjectMember member = members.findByProjectIdAndUserId(project.getId(), userId)
                .orElseThrow(() -> ApiException.notFound("El proyecto no está compartido con esa persona."));
        members.delete(member);
    }
}
