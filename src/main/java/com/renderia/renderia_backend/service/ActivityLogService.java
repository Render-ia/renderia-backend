package com.renderia.renderia_backend.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renderia.renderia_backend.validation.InputValidator;
import com.renderia.renderia_backend.dto.ActivityRequest;
import com.renderia.renderia_backend.dto.ActivityResponse;
import com.renderia.renderia_backend.exception.ApiException;
import com.renderia.renderia_backend.model.ActivityLog;
import com.renderia.renderia_backend.repository.ActivityLogRepository;
import com.renderia.renderia_backend.security.CurrentUser;

/** Historial de actividad. Cada usuario ve el suyo; el administrador ve el de todos. */
@Service
public class ActivityLogService {

    private static final int MAX_LIMIT = 100;

    private final ActivityLogRepository logs;

    public ActivityLogService(ActivityLogRepository logs) {
        this.logs = logs;
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> recent(CurrentUser user, Integer limit, Long userId) {
        PageRequest page = PageRequest.of(0, limit == null ? 20 : Math.min(MAX_LIMIT, Math.max(1, limit)));
        boolean seesAll = user.policy().canViewAllProjects();
        if (userId != null && !userId.equals(user.id()) && !seesAll) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo puedes ver tu propia actividad.");
        }
        Long filter = userId != null ? userId : (seesAll ? null : user.id());
        List<ActivityLog> result = filter == null
                ? logs.findAllByOrderByCreatedAtDesc(page)
                : logs.findAllByUserIdOrderByCreatedAtDesc(filter, page);
        return result.stream().map(ActivityResponse::from).toList();
    }

    @Transactional
    public ActivityResponse record(Long userId, String action, String details) {
        ActivityLog log = new ActivityLog();
        log.setUserId(userId);
        log.setAction(action.length() > 100 ? action.substring(0, 100) : action);
        log.setDetails(details);
        return ActivityResponse.from(logs.save(log));
    }

    @Transactional
    public ActivityResponse create(CurrentUser user, ActivityRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Envía la acción.");
        }
        String action = InputValidator.requiredText(request.action(), "La acción", 100);
        String details = InputValidator.optionalText(request.details(), "El detalle", 2000);
        return record(user.id(), action, details);
    }
}
