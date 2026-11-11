package com.renderia.renderia_backend.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.renderia.renderia_backend.model.BuildingType;
import com.renderia.renderia_backend.model.FloorPlan;
import com.renderia.renderia_backend.model.FloorPlanFile;
import com.renderia.renderia_backend.model.Project;
import com.renderia.renderia_backend.model.User;
import com.renderia.renderia_backend.repository.BuildingTypeRepository;
import com.renderia.renderia_backend.repository.FloorPlanFileRepository;
import com.renderia.renderia_backend.repository.FloorPlanRepository;
import com.renderia.renderia_backend.repository.ProjectRepository;
import com.renderia.renderia_backend.repository.UserRepository;

/**
 * Proyectos de ejemplo para las cuentas de prueba. Si una cuenta de prueba no
 * tiene proyectos, se le crean con el plano de la casa de ejemplo ya subido,
 * listo para analizar con IA durante la sustentación.
 */
@Component
@Order(20)
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final String SAMPLE_PLAN = "samples/house-floor-plan.svg";

    /** Proyecto de ejemplo: nombre, tipo de edificación, ubicación, descripción y si trae plano. */
    private record SampleProject(String name, String buildingType, String location, String description, boolean withPlan) {
    }

    private static final List<SampleProject> ENGINEER_PROJECTS = List.of(
            new SampleProject("Casa Los Andes", "Vivienda unifamiliar", "Pasto, Nariño",
                    "Vivienda de un piso con dos habitaciones, sala comedor, cocina y baño.", true),
            new SampleProject("Local Avenida Panamericana", "Local comercial", "Pasto, Nariño",
                    "Local de esquina en primer piso. Falta subir el plano.", false));

    private static final List<SampleProject> STUDENT_PROJECTS = List.of(
            new SampleProject("Taller de estructuras: casa tipo", "Vivienda unifamiliar", "Universidad Cooperativa, Pasto",
                    "Ejercicio de clase para identificar muros, columnas y vigas.", true));

    private final UserRepository users;
    private final ProjectRepository projects;
    private final BuildingTypeRepository buildingTypes;
    private final FloorPlanRepository floorPlans;
    private final FloorPlanFileRepository floorPlanFiles;
    private final String publicUrl;

    public DemoDataInitializer(UserRepository users, ProjectRepository projects, BuildingTypeRepository buildingTypes,
                               FloorPlanRepository floorPlans, FloorPlanFileRepository floorPlanFiles,
                               @Value("${app.public-url}") String publicUrl) {
        this.users = users;
        this.projects = projects;
        this.buildingTypes = buildingTypes;
        this.floorPlans = floorPlans;
        this.floorPlanFiles = floorPlanFiles;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            createSampleProjects(DemoAccountsInitializer.ENGINEER_EMAIL, ENGINEER_PROJECTS);
            createSampleProjects(DemoAccountsInitializer.STUDENT_EMAIL, STUDENT_PROJECTS);
        } catch (RuntimeException e) {
            log.warn("No se pudieron crear los proyectos de ejemplo: {}", e.getMessage());
        }
    }

    @Transactional
    void createSampleProjects(String email, List<SampleProject> samples) {
        User owner = users.findByEmailIgnoreCase(email).orElse(null);
        if (owner == null || projects.countByUserId(owner.getId()) > 0) {
            return;
        }
        byte[] planImage = readSamplePlan();
        for (SampleProject sample : samples) {
            Project project = new Project();
            project.setUserId(owner.getId());
            project.setName(sample.name());
            project.setLocation(sample.location());
            project.setDescription(sample.description());
            project.setBuildingTypeId(buildingTypes.findAll().stream()
                    .filter(type -> type.getName().equals(sample.buildingType()))
                    .map(BuildingType::getId)
                    .findFirst()
                    .orElse(null));
            project = projects.save(project);

            if (sample.withPlan()) {
                addSamplePlan(project, planImage);
            }
        }
        log.info("Proyectos de ejemplo creados para {}", email);
    }

    private void addSamplePlan(Project project, byte[] image) {
        FloorPlan plan = new FloorPlan();
        plan.setProjectId(project.getId());
        plan.setFileName("house-floor-plan.svg");
        plan.setFloorLevel(1);
        plan.setScale("1:100");
        plan.setFileUrl("");
        plan = floorPlans.saveAndFlush(plan);
        plan.setFileUrl(publicUrl + "/api/v1/floor-plans/" + plan.getId() + "/image");
        floorPlans.save(plan);
        floorPlanFiles.save(new FloorPlanFile(plan.getId(), "image/svg+xml", image));
    }

    private static byte[] readSamplePlan() {
        try (InputStream input = new ClassPathResource(SAMPLE_PLAN).getInputStream()) {
            return input.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No se encontró el plano de ejemplo " + SAMPLE_PLAN, e);
        }
    }
}
