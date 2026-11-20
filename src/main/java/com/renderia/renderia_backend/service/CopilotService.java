package com.renderia.renderia_backend.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.renderia.renderia_backend.ai.AiServiceClient;
import com.renderia.renderia_backend.dto.CopilotRequest;
import com.renderia.renderia_backend.dto.RenderRequest;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.AiAnalysis;
import com.renderia.renderia_backend.model.CatalogEntity;
import com.renderia.renderia_backend.model.CopilotMessage;
import com.renderia.renderia_backend.model.FloorPlan;
import com.renderia.renderia_backend.model.Project;
import com.renderia.renderia_backend.model.Role;
import com.renderia.renderia_backend.model.StructuralElement;
import com.renderia.renderia_backend.repository.CopilotMessageRepository;
import com.renderia.renderia_backend.repository.ElementTypeRepository;
import com.renderia.renderia_backend.repository.MaterialRepository;
import com.renderia.renderia_backend.repository.StructuralElementRepository;
import com.renderia.renderia_backend.security.CurrentUser;
import com.renderia.renderia_backend.security.quota.DailyQuota;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pattern: Facade — the copilot and the AI render seen from the backend.
 * It checks that the user can see the analysis, applies the per-user daily
 * quota, builds the project context from the database (so the browser cannot
 * fake it) and forwards the call to the AI service with the shared key.
 */
@Service
public class CopilotService {

    static final int MAX_QUESTION = 1000;
    static final int MAX_HISTORY = 10;
    static final int MAX_MESSAGE = 2000;
    static final int MAX_QUANTITIES = 30;
    static final int MAX_NOTES = 300;
    /** The AI service reads at most 600 elements; sending more only makes the request heavier. */
    static final int MAX_CONTEXT_ELEMENTS = 600;
    /** About 4.5 MB of image once decoded; the AI service checks the real type and size again. */
    static final int MAX_IMAGE_CHARS = 6_200_000;
    static final Set<String> STYLES = Set.of("modern", "tropical", "rustic", "contemporary", "minimalist", "traditional");
    static final Set<String> SCENES = Set.of("exterior", "interior");
    /** Saved answers and proposals are cut at these sizes so one reply cannot fill the database. */
    static final int MAX_SAVED_ANSWER = 20_000;
    static final int MAX_SAVED_PROPOSALS = 50_000;
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private final AiServiceClient ai;
    private final AnalysisService analyses;
    private final FloorPlanService floorPlans;
    private final ProjectService projects;
    private final StructuralElementRepository elements;
    private final ElementTypeRepository elementTypes;
    private final MaterialRepository materials;
    private final CopilotMessageRepository messages;
    private final DailyQuota quota;
    private final int copilotLimit;
    private final int studentCopilotLimit;
    private final int renderLimit;

    @Autowired
    public CopilotService(AiServiceClient ai, AnalysisService analyses, FloorPlanService floorPlans, ProjectService projects,
                          StructuralElementRepository elements, ElementTypeRepository elementTypes,
                          MaterialRepository materials, CopilotMessageRepository messages,
                          @Value("${app.ai.copilot-daily-limit:60}") int copilotLimit,
                          @Value("${app.ai.copilot-daily-limit-student:20}") int studentCopilotLimit,
                          @Value("${app.ai.render-daily-limit:5}") int renderLimit) {
        this(ai, analyses, floorPlans, projects, elements, elementTypes, materials, messages,
                new DailyQuota(Clock.systemUTC()), copilotLimit, studentCopilotLimit, renderLimit);
    }

    CopilotService(AiServiceClient ai, AnalysisService analyses, FloorPlanService floorPlans, ProjectService projects,
                   StructuralElementRepository elements, ElementTypeRepository elementTypes, MaterialRepository materials,
                   CopilotMessageRepository messages, DailyQuota quota, int copilotLimit, int studentCopilotLimit, int renderLimit) {
        this.ai = ai;
        this.analyses = analyses;
        this.floorPlans = floorPlans;
        this.projects = projects;
        this.elements = elements;
        this.elementTypes = elementTypes;
        this.materials = materials;
        this.messages = messages;
        this.quota = quota;
        this.copilotLimit = copilotLimit;
        this.studentCopilotLimit = studentCopilotLimit;
        this.renderLimit = renderLimit;
    }

    public Map<String, Object> ask(CurrentUser user, Long analysisId, CopilotRequest request) {
        String question = request == null || request.question() == null ? "" : request.question().strip();
        if (question.isEmpty()) {
            throw ApiException.badRequest("Escribe una pregunta.");
        }
        if (question.length() > MAX_QUESTION) {
            throw ApiException.badRequest("La pregunta admite máximo " + MAX_QUESTION + " caracteres.");
        }
        AiAnalysis analysis = analyses.findVisible(user, analysisId);
        quota.take("copilot:" + user.id(), copilotLimit(user),
                "Llegaste al límite diario de preguntas al copiloto. Vuelve a intentarlo mañana.");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("question", question);
        body.put("depth", "deep".equals(request.depth()) ? "deep" : "fast");
        body.put("history", history(request.history()));
        body.put("context", context(user, analysis, request.quantities()));
        Map<String, Object> answer = callAi("copilot:" + user.id(), "/assistant", body);
        remember(user, analysis.getId(), question, answer);
        return answer;
    }

    /** The user's saved conversation about this analysis, oldest first (up to the last 100 turns). */
    public List<Map<String, Object>> conversation(CurrentUser user, Long analysisId) {
        analyses.findVisible(user, analysisId);
        List<CopilotMessage> newestFirst = messages.findTop100ByAiAnalysisIdAndUserIdOrderByIdDesc(analysisId, user.id());
        List<Map<String, Object>> turns = new ArrayList<>();
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            CopilotMessage message = newestFirst.get(i);
            Map<String, Object> turn = new LinkedHashMap<>();
            turn.put("role", message.getRole());
            turn.put("text", message.getContent());
            turn.put("proposals", proposals(message.getProposals()));
            turn.put("createdAt", message.getCreatedAt());
            turns.add(turn);
        }
        return turns;
    }

    /** Saves the question and the answer, so the conversation is still there on the next visit. */
    void remember(CurrentUser user, Long analysisId, String question, Map<String, Object> answer) {
        Object text = answer == null ? null : answer.get("answer");
        if (!(text instanceof String reply) || reply.isBlank()) {
            return;
        }
        String proposals = null;
        Object list = answer.get("proposals");
        if (list instanceof List<?> items && !items.isEmpty()) {
            String json = JSON.writeValueAsString(items);
            proposals = json.length() > MAX_SAVED_PROPOSALS ? null : json;
        }
        messages.save(new CopilotMessage(analysisId, user.id(), CopilotMessage.USER, question, null));
        messages.save(new CopilotMessage(analysisId, user.id(), CopilotMessage.MODEL,
                reply.length() > MAX_SAVED_ANSWER ? reply.substring(0, MAX_SAVED_ANSWER) : reply, proposals));
    }

    private static Object proposals(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = JSON.readTree(stored);
            return node.isArray() ? node : List.of();
        } catch (JacksonException e) {
            return List.of();
        }
    }

    public Map<String, Object> render(CurrentUser user, Long analysisId, RenderRequest request) {
        if (request == null || request.image() == null || request.image().isBlank()) {
            throw ApiException.badRequest("Falta la captura del modelo 3D.");
        }
        if (request.image().length() > MAX_IMAGE_CHARS) {
            throw ApiException.badRequest("La captura es demasiado grande. Usa una de máximo 4 MB.");
        }
        if (request.style() == null || !STYLES.contains(request.style())) {
            throw ApiException.badRequest("Elige un estilo de render válido.");
        }
        String scene = SCENES.contains(request.scene()) ? request.scene() : "exterior";
        String notes = request.notes() == null ? "" : request.notes().strip();
        if (notes.length() > MAX_NOTES) {
            throw ApiException.badRequest("Las indicaciones admiten máximo " + MAX_NOTES + " caracteres.");
        }
        analyses.findVisible(user, analysisId);
        quota.take("render:" + user.id(), renderLimit,
                "Llegaste al límite diario de renders con IA. Vuelve a intentarlo mañana.");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("image", request.image());
        body.put("style", request.style());
        body.put("scene", scene);
        body.put("notes", notes);
        return callAi("render:" + user.id(), "/render", body);
    }

    /** A question or render that the AI could not answer does not count against the daily limit. */
    private Map<String, Object> callAi(String quotaKey, String path, Map<String, Object> body) {
        try {
            return ai.postJson(path, body);
        } catch (RuntimeException e) {
            quota.giveBack(quotaKey);
            throw e;
        }
    }

    /** AI capabilities plus what this user has left today. */
    public Map<String, Object> capabilities(CurrentUser user) {
        Map<String, Object> answer = new LinkedHashMap<>(ai.capabilities());
        answer.put("remaining", Map.of(
                "copilot", quota.remaining("copilot:" + user.id(), copilotLimit(user)),
                "render", quota.remaining("render:" + user.id(), renderLimit)));
        return answer;
    }

    private int copilotLimit(CurrentUser user) {
        return Role.STUDENT.equals(user.role()) ? studentCopilotLimit : copilotLimit;
    }

    static List<Map<String, String>> history(List<CopilotRequest.Message> messages) {
        if (messages == null) {
            return List.of();
        }
        List<Map<String, String>> clean = new ArrayList<>();
        for (CopilotRequest.Message message : messages.subList(Math.max(0, messages.size() - MAX_HISTORY), messages.size())) {
            if (message == null || message.text() == null || message.text().isBlank()) {
                continue;
            }
            String text = message.text().strip();
            clean.add(Map.of(
                    "role", "model".equals(message.role()) ? "model" : "user",
                    "text", text.length() > MAX_MESSAGE ? text.substring(0, MAX_MESSAGE) : text));
        }
        return clean;
    }

    private Map<String, Object> context(CurrentUser user, AiAnalysis analysis, List<CopilotRequest.Quantity> quantities) {
        FloorPlan plan = floorPlans.findVisible(user, analysis.getFloorPlanId());
        Project project = projects.findVisible(user, plan.getProjectId());
        Map<Long, String> types = names(elementTypes.findAll());
        Map<Long, String> materialNames = names(materials.findAll());

        List<Map<String, Object>> rows = new ArrayList<>();
        for (StructuralElement e : elements.findAllByAiAnalysisIdOrderByIdAsc(analysis.getId()).stream().limit(MAX_CONTEXT_ELEMENTS).toList()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", e.getId());
            row.put("type", types.getOrDefault(e.getElementTypeId(), "Otro"));
            row.put("material", e.getMaterialId() == null ? null : materialNames.get(e.getMaterialId()));
            row.put("startX", e.getStartX());
            row.put("startY", e.getStartY());
            row.put("endX", e.getEndX());
            row.put("endY", e.getEndY());
            row.put("width", e.getWidth());
            row.put("height", e.getHeight());
            row.put("thickness", e.getThickness());
            row.put("confidence", e.getConfidence());
            rows.add(row);
        }

        Map<String, Object> context = new LinkedHashMap<>();
        Map<String, Object> projectInfo = new LinkedHashMap<>();
        projectInfo.put("name", project.getName());
        projectInfo.put("location", project.getLocation());
        context.put("project", projectInfo);
        context.put("plan", Map.of("scale", plan.getScale() == null ? "" : plan.getScale(), "floorLevel", plan.getFloorLevel()));
        // The analysis summary (warnings, assumptions, rooms) as stored; the AI service treats it as data.
        String summary = analysis.getRawResponse();
        context.put("analysisSummary", summary == null ? "" : summary.length() > 20_000 ? summary.substring(0, 20_000) : summary);
        context.put("elements", rows);
        context.put("quantities", quantities(quantities));
        return context;
    }

    static List<Map<String, Object>> quantities(List<CopilotRequest.Quantity> quantities) {
        if (quantities == null) {
            return List.of();
        }
        return quantities.stream()
                .filter(q -> q != null && q.item() != null && q.quantity() != null && Double.isFinite(q.quantity()))
                .limit(MAX_QUANTITIES)
                .map(q -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("item", q.item().length() > 80 ? q.item().substring(0, 80) : q.item());
                    row.put("unit", q.unit() == null ? "" : q.unit().length() > 8 ? q.unit().substring(0, 8) : q.unit());
                    row.put("quantity", q.quantity());
                    if (q.total() != null && Double.isFinite(q.total())) {
                        row.put("totalCop", q.total());
                    }
                    return row;
                })
                .collect(Collectors.toList());
    }

    private static Map<Long, String> names(List<? extends CatalogEntity> rows) {
        return rows.stream().collect(Collectors.toMap(CatalogEntity::getId, CatalogEntity::getName, (a, b) -> a));
    }

}
