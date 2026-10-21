package com.renderia.renderia_backend.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.renderia.renderia_backend.dto.FloorPlanResponse;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.FloorPlan;
import com.renderia.renderia_backend.model.FloorPlanFile;
import com.renderia.renderia_backend.model.Project;
import com.renderia.renderia_backend.repository.FloorPlanFileRepository;
import com.renderia.renderia_backend.repository.FloorPlanRepository;
import com.renderia.renderia_backend.repository.ProjectRepository;
import com.renderia.renderia_backend.security.CurrentUser;

/** Subir, listar, ver y borrar planos, siempre con los permisos del proyecto. */
@Service
public class FloorPlanService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/svg+xml");
    private static final Pattern SCALE = Pattern.compile("^1:\\d{1,5}$");

    private final FloorPlanRepository plans;
    private final FloorPlanFileRepository files;
    private final ProjectRepository projects;
    private final ProjectService projectService;
    private final String publicUrl;

    public FloorPlanService(FloorPlanRepository plans, FloorPlanFileRepository files, ProjectRepository projects,
                            ProjectService projectService, @Value("${app.public-url}") String publicUrl) {
        this.plans = plans;
        this.files = files;
        this.projects = projects;
        this.projectService = projectService;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    /** Planos de todos los proyectos que el usuario puede ver, los más recientes primero. */
    @Transactional(readOnly = true)
    public List<FloorPlanResponse> listVisible(CurrentUser user) {
        List<FloorPlan> result;
        if (user.policy().canViewAllProjects()) {
            result = plans.findAllByOrderByUploadedAtDesc();
        } else {
            List<Long> own = projects.findAllByUserIdOrderByCreatedAtDesc(user.id()).stream()
                    .map(Project::getId)
                    .toList();
            result = own.isEmpty() ? List.of() : plans.findAllByProjectIdInOrderByUploadedAtDesc(own);
        }
        return result.stream().map(FloorPlanResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<FloorPlanResponse> listByProject(CurrentUser user, Long projectId) {
        projectService.findVisible(user, projectId);
        return plans.findAllByProjectIdOrderByFloorLevelAsc(projectId).stream()
                .map(FloorPlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FloorPlanResponse get(CurrentUser user, Long id) {
        return FloorPlanResponse.from(findVisible(user, id));
    }

    @Transactional
    public FloorPlanResponse upload(CurrentUser user, Long projectId, MultipartFile file, Integer floorLevel, String scale) {
        projectService.findManageable(user, projectId);

        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Selecciona la imagen del plano.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw ApiException.badRequest("Formato no válido. Sube el plano en PNG, JPG, WEBP o SVG.");
        }
        if (floorLevel == null || floorLevel < 1 || floorLevel > 200) {
            throw ApiException.badRequest("El piso debe ser un número entre 1 y 200.");
        }
        if (scale == null || !SCALE.matcher(scale.trim()).matches()) {
            throw ApiException.badRequest("La escala debe escribirse como 1:100.");
        }

        FloorPlan plan = new FloorPlan();
        plan.setProjectId(projectId);
        plan.setFileName(cleanFileName(file.getOriginalFilename()));
        plan.setFloorLevel(floorLevel);
        plan.setScale(scale.trim());
        plan.setFileUrl("");
        plan = plans.saveAndFlush(plan);

        plan.setFileUrl(publicUrl + "/api/v1/floor-plans/" + plan.getId() + "/image");
        try {
            files.save(new FloorPlanFile(plan.getId(), contentType, file.getBytes()));
        } catch (IOException e) {
            throw ApiException.badRequest("No se pudo leer el archivo del plano.");
        }
        return FloorPlanResponse.from(plans.save(plan));
    }

    @Transactional
    public void delete(CurrentUser user, Long id) {
        FloorPlan plan = findVisible(user, id);
        projectService.findManageable(user, plan.getProjectId());
        plans.delete(plan);
    }

    /** Imagen del plano, para mostrarla en el navegador y para que la IA la analice. */
    @Transactional(readOnly = true)
    public FloorPlanFile image(Long id) {
        return files.findById(id).orElseThrow(() -> ApiException.notFound("El plano no tiene imagen."));
    }

    private FloorPlan findVisible(CurrentUser user, Long id) {
        FloorPlan plan = plans.findById(id)
                .orElseThrow(() -> ApiException.notFound("El plano no existe o no tienes acceso a él."));
        projectService.findVisible(user, plan.getProjectId());
        return plan;
    }

    private static String cleanFileName(String name) {
        if (name == null || name.isBlank()) {
            return "plano";
        }
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        return base.length() > 255 ? base.substring(0, 255) : base;
    }
}
