package com.renderia.renderia_backend.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renderia.renderia_backend.dto.ReviewResponse;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.AiAnalysis;
import com.renderia.renderia_backend.model.User;
import com.renderia.renderia_backend.repository.UserRepository;
import com.renderia.renderia_backend.security.CurrentUser;

/**
 * A professional's sign-off on an analysis. The owner (or a manager) and the
 * Reviewer role can sign; the server stamps who and when, so it cannot be faked
 * from the browser. It is recorded in the activity log.
 */
@Service
public class ReviewService {

    private final AnalysisService analyses;
    private final ActivityLogService activity;
    private final UserRepository users;

    public ReviewService(AnalysisService analyses, ActivityLogService activity, UserRepository users) {
        this.analyses = analyses;
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
        activity.record(user.id(), "Análisis revisado",
                "Análisis #" + analysis.getId() + " revisado por " + reviewer.getFullName() + " (" + user.role() + ")");
        return new ReviewResponse(analysis.getId(), reviewer.getId(), reviewer.getFullName(), now);
    }
}
