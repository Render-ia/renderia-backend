package com.renderia.renderia_backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renderia.renderia_backend.validation.InputValidator;
import com.renderia.renderia_backend.dto.ProjectRequest;
import com.renderia.renderia_backend.dto.ProjectResponse;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.Project;
import com.renderia.renderia_backend.repository.BuildingTypeRepository;
import com.renderia.renderia_backend.repository.ProjectMemberRepository;
import com.renderia.renderia_backend.repository.ProjectRepository;
import com.renderia.renderia_backend.security.CurrentUser;
import com.renderia.renderia_backend.security.policy.RolePolicy;

/**
 * Proyectos que cada usuario puede ver y cambiar.
 * Las reglas por rol vienen de la política (patrón Strategy), no de if por rol.
 */
@Service
public class ProjectService {

    private final ProjectRepository projects;
    private final BuildingTypeRepository buildingTypes;

    private final ProjectMemberRepository members;

    public ProjectService(ProjectRepository projects, BuildingTypeRepository buildingTypes, ProjectMemberRepository members) {
        this.projects = projects;
        this.buildingTypes = buildingTypes;
        this.members = members;
    }

    /** Sin userId: todos (solo quien puede ver todo) o los propios y compartidos. Con userId: los de ese usuario. */
    @Transactional(readOnly = true)
    public List<ProjectResponse> list(CurrentUser user, Long userId) {
        RolePolicy policy = user.policy();
        List<Project> result;
        if (userId == null) {
            result = policy.canViewAllProjects()
                    ? projects.findAllByOrderByCreatedAtDesc()
                    : projects.findAccessible(user.id());
        } else {
            if (!userId.equals(user.id()) && !policy.canViewAllProjects()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Solo puedes ver tus propios proyectos.");
            }
            result = projects.findAllByUserIdOrderByCreatedAtDesc(userId);
        }
        return result.stream().map(ProjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(CurrentUser user, Long id) {
        return ProjectResponse.from(findVisible(user, id));
    }

    @Transactional
    public ProjectResponse create(CurrentUser user, ProjectRequest request) {
        int limit = user.policy().projectLimit();
        if (limit <= 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tu rol no permite crear proyectos.");
        }
        if (projects.countByUserId(user.id()) >= limit) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Tu rol permite máximo " + limit + " proyectos. Elimina uno para crear otro.");
        }
        Project project = new Project();
        project.setUserId(user.id());
        apply(project, request);
        return ProjectResponse.from(projects.save(project));
    }

    @Transactional
    public ProjectResponse update(CurrentUser user, Long id, ProjectRequest request) {
        Project project = findManageable(user, id);
        apply(project, request);
        return ProjectResponse.from(projects.saveAndFlush(project));
    }

    @Transactional
    public void delete(CurrentUser user, Long id) {
        projects.delete(findManageable(user, id));
    }

    /** Proyecto que el usuario puede ver; si no puede, responde 404 para no revelar que existe. */
    public Project findVisible(CurrentUser user, Long id) {
        Project project = projects.findById(id)
                .orElseThrow(() -> ApiException.notFound("El proyecto no existe o no tienes acceso a él."));
        if (!canSee(user, project)) {
            throw ApiException.notFound("El proyecto no existe o no tienes acceso a él.");
        }
        return project;
    }

    /** Owner, a role that sees everything, or a user the project was shared with. */
    public boolean canSee(CurrentUser user, Project project) {
        return project.getUserId().equals(user.id())
                || user.policy().canViewAllProjects()
                || members.existsByProjectIdAndUserId(project.getId(), user.id());
    }

    /** Proyecto que el usuario puede modificar: el suyo, o cualquiera si su rol lo permite. */
    public Project findManageable(CurrentUser user, Long id) {
        Project project = findVisible(user, id);
        if (!project.getUserId().equals(user.id()) && !user.policy().canManageAnyProject()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el dueño del proyecto puede modificarlo.");
        }
        return project;
    }

    private void apply(Project project, ProjectRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Envía los datos del proyecto.");
        }
        if (request.buildingTypeId() != null && !buildingTypes.existsById(request.buildingTypeId())) {
            throw ApiException.badRequest("El tipo de edificación no existe.");
        }
        project.setName(InputValidator.requiredText(request.name(), "El nombre del proyecto", 150));
        project.setDescription(InputValidator.optionalText(request.description(), "La descripción", 2000));
        project.setLocation(InputValidator.optionalText(request.location(), "La ubicación", 150));
        project.setBuildingTypeId(request.buildingTypeId());
    }
}
