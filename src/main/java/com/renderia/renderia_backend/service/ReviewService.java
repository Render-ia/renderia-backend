package com.renderia.renderia_backend.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renderia.renderia_backend.dto.ReviewResponse;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.AiAnalysis;
import com.renderia.renderia_backend.model.User;
import com.renderia.renderia_backend.repository.AiAnalysisRepository;
import com.renderia.renderia_backend.repository.UserRepository;
import com.renderia.renderia_backend.security.CurrentUser;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * A professional's sign-off on an analysis. The owner (or a manager) and the
 * Reviewer role can sign; the server stamps who and when inside the analysis
 * summary, so it cannot be faked from the browser. It also goes to the activity log.
 */
@Service
public class ReviewService {

    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private final AnalysisService analyses;
    private final AiAnalysisRepository analysisRepository;
    private final ActivityLogService activity;
    private final UserRepository users;

    public ReviewService(AnalysisService analyses, AiAnalysisRepository analysisRepository,
                         ActivityLogService activity, UserRepository users) {
        this.analyses = analyses;
        this.analysisRepository = analysisRepository;
        this.activity = activity;
        this.users = users;
    }

    @Transactional
    public ReviewResponse review(CurrentUser user, Long analysisId) {
        AiAnalysis analysis = user.policy().canReviewAnyProject()
                ? analyses.findVisible(user, analysisId)
                : analyses.findManageable(user, analysisId);
        User reviewer = users.findById(user.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Tu sesión no es válida."));
        Instant now = Instant.now();

        analysis.setRawResponse(withValidation(analysis.getRawResponse(), reviewer.getFullName(), now));
        analysisRepository.save(analysis);
        activity.record(user.id(), "Análisis revisado",
                "Análisis #" + analysis.getId() + " revisado por " + reviewer.getFullName() + " (" + user.role() + ")");
        return new ReviewResponse(analysis.getId(), reviewer.getId(), reviewer.getFullName(), now);
    }

    /** Adds { validation: { reviewedAt, reviewedBy } } to the stored summary, keeping the rest. */
    static String withValidation(String rawResponse, String reviewerName, Instant when) {
        ObjectNode summary;
        try {
            JsonNode parsed = rawResponse == null || rawResponse.isBlank() ? null : JSON.readTree(rawResponse);
            summary = parsed instanceof ObjectNode object ? object : JSON.createObjectNode();
        } catch (RuntimeException e) {
            summary = JSON.createObjectNode();
        }
        summary.putObject("validation")
                .put("reviewedAt", when.toString())
                .put("reviewedBy", reviewerName);
        return JSON.writeValueAsString(summary);
    }
}
