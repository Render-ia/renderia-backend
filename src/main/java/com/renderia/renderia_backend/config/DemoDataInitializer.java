package com.renderia.renderia_backend.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.renderia.renderia_backend.model.AiAnalysis;
import com.renderia.renderia_backend.model.AiModel;
import com.renderia.renderia_backend.model.AnalysisStatus;
import com.renderia.renderia_backend.model.BuildingType;
import com.renderia.renderia_backend.model.CatalogEntity;
import com.renderia.renderia_backend.model.FloorPlan;
import com.renderia.renderia_backend.model.FloorPlanFile;
import com.renderia.renderia_backend.model.Model3D;
import com.renderia.renderia_backend.model.Project;
import com.renderia.renderia_backend.model.ProjectMember;
import com.renderia.renderia_backend.model.Role;
import com.renderia.renderia_backend.model.StructuralElement;
import com.renderia.renderia_backend.model.User;
import com.renderia.renderia_backend.repository.AiAnalysisRepository;
import com.renderia.renderia_backend.repository.AiModelRepository;
import com.renderia.renderia_backend.repository.BuildingTypeRepository;
import com.renderia.renderia_backend.repository.ElementTypeRepository;
import com.renderia.renderia_backend.repository.FloorPlanFileRepository;
import com.renderia.renderia_backend.repository.FloorPlanRepository;
import com.renderia.renderia_backend.repository.MaterialRepository;
import com.renderia.renderia_backend.repository.Model3DRepository;
import com.renderia.renderia_backend.repository.ProjectMemberRepository;
import com.renderia.renderia_backend.repository.ProjectRepository;
import com.renderia.renderia_backend.repository.StructuralElementRepository;
import com.renderia.renderia_backend.repository.UserRepository;
import com.renderia.renderia_backend.security.DemoAccounts;
import com.renderia.renderia_backend.security.policy.StudentPolicy;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sample projects for the demo accounts, each with a different kind of building
 * (house, apartment building, school, health center, retail store, warehouse) so
 * the 3D viewer shows different models. Most come already analyzed with the
 * local vision detector (the result is stored next to the plan in
 * resources/samples), and one is left without analysis so the full cycle can be
 * run live. Missing projects, plans, analyses and shares are added on every
 * start; nothing that already exists is changed.
 */
@Component
@Order(20)
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final ObjectMapper JSON = JsonMapper.builder().build();
    private static final String LOCAL_VISION = "local-vision";

    /** A sample plan in resources/samples; analyzed = it ships with its stored analysis. */
    private enum SamplePlan {
        HOUSE("house-floor-plan", true),
        HOUSE_SECOND_FLOOR("house-second-floor-plan", true),
        APARTMENT_GROUND_FLOOR("apartment-building-ground-floor-plan", true),
        APARTMENT_BUILDING("apartment-building-floor-plan", true),
        SCHOOL("school-floor-plan", true),
        SCHOOL_SECOND_FLOOR("school-second-floor-plan", true),
        HEALTH_CENTER("health-center-floor-plan", true),
        HEALTH_CENTER_SECOND_FLOOR("health-center-second-floor-plan", true),
        RETAIL_STORE("retail-store-floor-plan", false),
        WAREHOUSE("warehouse-floor-plan", true);

        final String file;
        final boolean analyzed;

        SamplePlan(String file, boolean analyzed) {
            this.file = file;
            this.analyzed = analyzed;
        }
    }

    /**
     * A sample project; floors.get(0) is floor 1, floors.get(1) floor 2, and so on.
     * A building can repeat the same plan on several floors (typical floor).
     */
    private record SampleProject(String ownerRole, String name, String buildingType, String location,
                                 String description, List<SamplePlan> floors, boolean sharedWithClient) {
    }

    private static List<SamplePlan> floors(SamplePlan... plans) {
        return List.of(plans);
    }

    private static final List<SampleProject> SAMPLES = List.of(
            new SampleProject(Role.ENGINEER, "Casa Los Andes", "Vivienda unifamiliar", "Pasto, Nariño",
                    "Casa de dos pisos: zona social abajo y tres alcobas con estudio arriba.",
                    floors(SamplePlan.HOUSE, SamplePlan.HOUSE_SECOND_FLOOR), true),
            new SampleProject(Role.ENGINEER, "Edificio Mirador del Volcán", "Edificio residencial", "Pasto, Nariño",
                    "Edificio de seis pisos: locales y portería en el primero, dos apartamentos por piso del segundo al sexto.",
                    floors(SamplePlan.APARTMENT_GROUND_FLOOR, SamplePlan.APARTMENT_BUILDING, SamplePlan.APARTMENT_BUILDING,
                            SamplePlan.APARTMENT_BUILDING, SamplePlan.APARTMENT_BUILDING, SamplePlan.APARTMENT_BUILDING), true),
            new SampleProject(Role.ENGINEER, "Centro de Salud San Rafael", "Centro de salud", "Ipiales, Nariño",
                    "Dos pisos: consultorios, urgencias y farmacia abajo; hospitalización arriba.",
                    floors(SamplePlan.HEALTH_CENTER, SamplePlan.HEALTH_CENTER_SECOND_FLOOR), false),
            new SampleProject(Role.ENGINEER, "Local Avenida Panamericana", "Local comercial", "Pasto, Nariño",
                    "Local con vitrina al frente, depósito y baño. Listo para analizar con IA en vivo.",
                    floors(SamplePlan.RETAIL_STORE), false),
            new SampleProject(Role.STUDENT, "Taller de estructuras: casa tipo", "Vivienda unifamiliar",
                    "Universidad Cooperativa, Pasto",
                    "Ejercicio de clase para identificar muros, columnas y vigas.",
                    floors(SamplePlan.HOUSE), false),
            new SampleProject(Role.STUDENT, "Colegio Los Pinos", "Institución educativa", "Túquerres, Nariño",
                    "Dos pisos: cuatro aulas abajo; laboratorio, biblioteca y sala de profesores arriba.",
                    floors(SamplePlan.SCHOOL, SamplePlan.SCHOOL_SECOND_FLOOR), false),
            new SampleProject(Role.ADMIN, "Bodega Parque Industrial", "Bodega", "Pasto, Nariño",
                    "Nave de gran luz con columnas cada 6 m y oficina en una esquina.",
                    floors(SamplePlan.WAREHOUSE), false));

    private final UserRepository users;
    private final ProjectRepository projects;
    private final BuildingTypeRepository buildingTypes;
    private final FloorPlanRepository floorPlans;
    private final FloorPlanFileRepository floorPlanFiles;
    private final ProjectMemberRepository members;
    private final AiModelRepository aiModels;
    private final AiAnalysisRepository analyses;
    private final StructuralElementRepository elements;
    private final ElementTypeRepository elementTypes;
    private final MaterialRepository materials;
    private final Model3DRepository models3d;
    private final String publicUrl;

    public DemoDataInitializer(UserRepository users, ProjectRepository projects, BuildingTypeRepository buildingTypes,
                               FloorPlanRepository floorPlans, FloorPlanFileRepository floorPlanFiles,
                               ProjectMemberRepository members, AiModelRepository aiModels,
                               AiAnalysisRepository analyses, StructuralElementRepository elements,
                               ElementTypeRepository elementTypes, MaterialRepository materials,
                               Model3DRepository models3d, @Value("${app.public-url}") String publicUrl) {
        this.users = users;
        this.projects = projects;
        this.buildingTypes = buildingTypes;
        this.floorPlans = floorPlans;
        this.floorPlanFiles = floorPlanFiles;
        this.members = members;
        this.aiModels = aiModels;
        this.analyses = analyses;
        this.elements = elements;
        this.elementTypes = elementTypes;
        this.materials = materials;
        this.models3d = models3d;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    @Override
    public void run(ApplicationArguments args) {
        for (SampleProject sample : SAMPLES) {
            try {
                ensureSample(sample);
            } catch (RuntimeException e) {
                log.warn("No se pudo preparar el proyecto de ejemplo \"{}\": {}", sample.name(), e.getMessage());
            }
        }
    }

    private void ensureSample(SampleProject sample) {
        User owner = users.findByEmailIgnoreCase(DemoAccounts.emailOf(sample.ownerRole())).orElse(null);
        if (owner == null) {
            return;
        }
        Project project = findOrCreateProject(owner, sample);
        if (project == null) {
            return;
        }
        List<FloorPlan> plans = floorPlans.findAllByProjectIdOrderByFloorLevelAsc(project.getId());
        for (int level = 1; level <= sample.floors().size(); level++) {
            SamplePlan floor = sample.floors().get(level - 1);
            int wanted = level;
            FloorPlan plan = plans.stream().filter(p -> p.getFloorLevel() == wanted).findFirst()
                    .orElseGet(() -> addPlan(project, floor, wanted));
            if (floor.analyzed && plan.getFileName().equals(floor.file + ".svg")
                    && !analyses.existsByFloorPlanIdAndStatus(plan.getId(), AnalysisStatus.COMPLETED)) {
                addStoredAnalysis(plan, floor);
            }
        }
        if (sample.sharedWithClient()) {
            shareWithDemoClient(project);
        }
    }

    private Project findOrCreateProject(User owner, SampleProject sample) {
        List<Project> owned = projects.findAllByUserIdOrderByCreatedAtDesc(owner.getId());
        Optional<Project> existing = owned.stream().filter(p -> p.getName().equals(sample.name())).findFirst();
        if (existing.isPresent()) {
            // Keep the sample text in step with its floors (the sample can gain floors over time).
            Project project = existing.get();
            if (!sample.description().equals(project.getDescription())) {
                project.setDescription(sample.description());
                return projects.save(project);
            }
            return project;
        }
        // Students keep their project limit even for sample data.
        if (Role.STUDENT.equals(sample.ownerRole()) && owned.size() >= StudentPolicy.MAX_PROJECTS) {
            return null;
        }
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
        log.info("Proyecto de ejemplo creado: {}", sample.name());
        return projects.save(project);
    }

    private FloorPlan addPlan(Project project, SamplePlan sample, int floorLevel) {
        FloorPlan plan = new FloorPlan();
        plan.setProjectId(project.getId());
        plan.setFileName(sample.file + ".svg");
        plan.setFloorLevel(floorLevel);
        plan.setScale("1:100");
        plan.setFileUrl("");
        plan = floorPlans.saveAndFlush(plan);
        plan.setFileUrl(publicUrl + "/api/v1/floor-plans/" + plan.getId() + "/image");
        plan = floorPlans.save(plan);
        floorPlanFiles.save(new FloorPlanFile(plan.getId(), "image/svg+xml", read(sample.file + ".svg")));
        return plan;
    }

    /** Saves the analysis the local vision detector gives for this plan (same elements and summary). */
    private void addStoredAnalysis(FloorPlan plan, SamplePlan sample) {
        AiModel model = aiModels.findAll().stream()
                .filter(m -> LOCAL_VISION.equals(m.getProvider()))
                .findFirst()
                .orElse(null);
        if (model == null) {
            return;
        }
        JsonNode stored = JSON.readTree(read(sample.file + ".analysis.json"));
        Map<String, Long> typeIds = idsByName(elementTypes.findAll());
        Map<String, Long> materialIds = idsByName(materials.findAll());

        AiAnalysis analysis = new AiAnalysis();
        analysis.setFloorPlanId(plan.getId());
        analysis.setAiModelId(model.getId());
        analysis.setStatus(AnalysisStatus.COMPLETED);
        analysis.setRawResponse(JSON.writeValueAsString(stored.get("summary")));
        analysis.setDurationMs(900);
        analysis = analyses.save(analysis);

        int count = 0;
        for (JsonNode node : stored.get("elements")) {
            Long typeId = typeIds.get(node.path("type").asString());
            if (typeId == null) {
                continue;
            }
            StructuralElement element = new StructuralElement();
            element.setAiAnalysisId(analysis.getId());
            element.setElementTypeId(typeId);
            element.setMaterialId(node.path("material").isString() ? materialIds.get(node.path("material").asString()) : null);
            element.setStartX(number(node, "startX"));
            element.setStartY(number(node, "startY"));
            element.setEndX(number(node, "endX"));
            element.setEndY(number(node, "endY"));
            element.setWidth(number(node, "width"));
            element.setHeight(number(node, "height"));
            element.setThickness(number(node, "thickness"));
            element.setConfidence(number(node, "confidence"));
            elements.save(element);
            count++;
        }

        Model3D model3d = new Model3D();
        model3d.setFloorPlanId(plan.getId());
        model3d.setAiAnalysisId(analysis.getId());
        model3d.setFormat("GLB");
        model3d.setElementCount(count);
        models3d.save(model3d);
        log.info("Análisis de ejemplo guardado para {} ({} elementos)", sample.file, count);
    }

    private void shareWithDemoClient(Project project) {
        users.findByEmailIgnoreCase(DemoAccounts.emailOf(Role.CLIENT))
                .filter(client -> !members.existsByProjectIdAndUserId(project.getId(), client.getId()))
                .ifPresent(client -> members.save(new ProjectMember(project.getId(), client.getId())));
    }

    private static Map<String, Long> idsByName(List<? extends CatalogEntity> rows) {
        return rows.stream().collect(Collectors.toMap(CatalogEntity::getName, CatalogEntity::getId, (a, b) -> a));
    }

    private static Double number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asDouble();
    }

    private static byte[] read(String file) {
        try (InputStream input = new ClassPathResource("samples/" + file).getInputStream()) {
            return input.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No se encontró el archivo de ejemplo " + file, e);
        }
    }
}
