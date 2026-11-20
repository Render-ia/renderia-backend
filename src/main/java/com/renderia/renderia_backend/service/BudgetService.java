package com.renderia.renderia_backend.service;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.BudgetSettings;
import com.renderia.renderia_backend.repository.BudgetSettingsRepository;
import com.renderia.renderia_backend.security.CurrentUser;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Budget inputs of an analysis (parameters, unit prices and AIU), saved in the
 * database so they are the same on every device. Anyone who can see the
 * analysis reads them; only who can edit it saves them.
 */
@Service
public class BudgetService {

    static final Set<String> GROUPS = Set.of("params", "prices", "aiu");
    static final int MAX_KEYS = 60;
    static final double MAX_VALUE = 1e12;
    static final int MAX_CHARS = 20_000;
    private static final Pattern KEY = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,40}$");
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private final AnalysisService analyses;
    private final BudgetSettingsRepository settings;

    public BudgetService(AnalysisService analyses, BudgetSettingsRepository settings) {
        this.analyses = analyses;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public JsonNode get(CurrentUser user, Long analysisId) {
        analyses.findVisible(user, analysisId);
        return settings.findById(analysisId)
                .map(saved -> parse(saved.getInputs()))
                .orElseGet(BudgetService::empty);
    }

    @Transactional
    public JsonNode save(CurrentUser user, Long analysisId, JsonNode body) {
        analyses.findManageable(user, analysisId);
        ObjectNode clean = validated(body);
        BudgetSettings row = settings.findById(analysisId).orElseGet(() -> new BudgetSettings(analysisId));
        row.setInputs(JSON.writeValueAsString(clean));
        row.setUpdatedBy(user.id());
        settings.save(row);
        return clean;
    }

    /** Only the three groups, each a small map of plain names to numbers between 0 and 10^12. */
    static ObjectNode validated(JsonNode body) {
        if (!(body instanceof ObjectNode object)) {
            throw ApiException.badRequest("Los datos del presupuesto deben ser un objeto JSON.");
        }
        if (object.toString().length() > MAX_CHARS) {
            throw ApiException.badRequest("Los datos del presupuesto son demasiado grandes.");
        }
        ObjectNode clean = empty();
        for (Map.Entry<String, JsonNode> group : object.properties()) {
            if (!GROUPS.contains(group.getKey())) {
                throw ApiException.badRequest("El presupuesto no tiene el grupo \"" + group.getKey() + "\".");
            }
            if (!(group.getValue() instanceof ObjectNode values) || values.size() > MAX_KEYS) {
                throw ApiException.badRequest("Los valores de \"" + group.getKey() + "\" no son válidos.");
            }
            ObjectNode target = (ObjectNode) clean.get(group.getKey());
            for (Map.Entry<String, JsonNode> value : values.properties()) {
                JsonNode number = value.getValue();
                if (!KEY.matcher(value.getKey()).matches() || !number.isNumber()
                        || !Double.isFinite(number.asDouble()) || number.asDouble() < 0 || number.asDouble() > MAX_VALUE) {
                    throw ApiException.badRequest("El valor de \"" + value.getKey() + "\" debe ser un número entre 0 y 10^12.");
                }
                target.put(value.getKey(), number.asDouble());
            }
        }
        return clean;
    }

    private static ObjectNode empty() {
        ObjectNode node = JSON.createObjectNode();
        GROUPS.stream().sorted().forEach(node::putObject);
        return node;
    }

    private static JsonNode parse(String stored) {
        try {
            return validated(JSON.readTree(stored));
        } catch (JacksonException | ApiException e) {
            return empty();
        }
    }
}
