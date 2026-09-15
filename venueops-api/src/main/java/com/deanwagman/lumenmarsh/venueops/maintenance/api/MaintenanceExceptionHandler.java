package com.deanwagman.lumenmarsh.venueops.maintenance.api;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionNotFoundException;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentNotFoundException;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.ConflictingMaintenanceCommandException;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceAssetNotFoundException;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceRecommendationNotFoundException;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceWorkOrderNotFoundException;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.StaleMaintenanceRecommendationVersionException;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.StaleMaintenanceWorkOrderVersionException;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.InvalidMaintenanceRecommendationTransitionException;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.InvalidMaintenanceTransitionException;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenancePrerequisiteException;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceSupervisorRequiredException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;

@RestControllerAdvice(basePackages = "com.deanwagman.lumenmarsh.venueops.maintenance")
public class MaintenanceExceptionHandler {

    public static final String CODE_NOT_FOUND = "MAINTENANCE_NOT_FOUND";
    public static final String CODE_INVALID_REQUEST = "INVALID_REQUEST";
    public static final String CODE_INVALID_TRANSITION = "INVALID_TRANSITION";
    public static final String CODE_STALE_VERSION = "STALE_VERSION";
    public static final String CODE_DUPLICATE_COMMAND = "DUPLICATE_COMMAND";
    public static final String CODE_PREREQUISITE = "MAINTENANCE_PREREQUISITE";
    public static final String CODE_INTERNAL_ERROR = "INTERNAL_ERROR";

    @ExceptionHandler({
            MaintenanceAssetNotFoundException.class,
            MaintenanceWorkOrderNotFoundException.class,
            MaintenanceRecommendationNotFoundException.class
    })
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return problem(HttpStatus.NOT_FOUND, CODE_NOT_FOUND, "Maintenance resource not found", ex.getMessage());
    }

    @ExceptionHandler(IncidentNotFoundException.class)
    public ProblemDetail handleIncidentNotFound(IncidentNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "INCIDENT_NOT_FOUND", "Incident not found", ex.getMessage());
    }

    @ExceptionHandler(AttractionNotFoundException.class)
    public ProblemDetail handleAttractionNotFound(AttractionNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "ATTRACTION_NOT_FOUND", "Attraction not found", ex.getMessage());
    }

    @ExceptionHandler(InvalidMaintenanceTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidMaintenanceTransitionException ex) {
        ProblemDetail detail = problem(
                HttpStatus.CONFLICT,
                CODE_INVALID_TRANSITION,
                "Invalid maintenance transition",
                ex.getMessage()
        );
        detail.setType(URI.create("https://lumen-marsh.dev/problems/invalid-maintenance-transition"));
        detail.setProperty("currentStatus", ex.currentStatus().name());
        detail.setProperty("command", ex.command().name());
        return detail;
    }

    @ExceptionHandler(InvalidMaintenanceRecommendationTransitionException.class)
    public ProblemDetail handleInvalidRecommendation(InvalidMaintenanceRecommendationTransitionException ex) {
        ProblemDetail detail = problem(
                HttpStatus.CONFLICT,
                CODE_INVALID_TRANSITION,
                "Invalid recommendation transition",
                ex.getMessage()
        );
        detail.setProperty("currentStatus", ex.currentStatus().name());
        detail.setProperty("command", ex.command().name());
        return detail;
    }

    @ExceptionHandler({
            StaleMaintenanceWorkOrderVersionException.class,
            StaleMaintenanceRecommendationVersionException.class
    })
    public ProblemDetail handleStaleVersion(RuntimeException ex) {
        ProblemDetail detail = problem(
                HttpStatus.CONFLICT,
                CODE_STALE_VERSION,
                "Stale version",
                ex.getMessage()
        );
        detail.setType(URI.create("https://lumen-marsh.dev/problems/stale-version"));
        if (ex instanceof StaleMaintenanceWorkOrderVersionException staleWorkOrder) {
            detail.setTitle("Stale work-order version");
            detail.setProperty("currentVersion", staleWorkOrder.actualVersion());
            detail.setProperty("expectedVersion", staleWorkOrder.expectedVersion());
        } else if (ex instanceof StaleMaintenanceRecommendationVersionException staleRecommendation) {
            detail.setTitle("Stale recommendation version");
            detail.setProperty("currentVersion", staleRecommendation.actualVersion());
            detail.setProperty("expectedVersion", staleRecommendation.expectedVersion());
        }
        return detail;
    }

    @ExceptionHandler(ConflictingMaintenanceCommandException.class)
    public ProblemDetail handleDuplicateCommand(ConflictingMaintenanceCommandException ex) {
        ProblemDetail detail = problem(
                HttpStatus.CONFLICT,
                CODE_DUPLICATE_COMMAND,
                "Duplicate command",
                ex.getMessage()
        );
        detail.setProperty("commandId", ex.commandId().toString());
        detail.setProperty("existingAggregateId", ex.existingAggregateId());
        detail.setProperty("requestedAggregateId", ex.requestedAggregateId());
        return detail;
    }

    @ExceptionHandler(MaintenancePrerequisiteException.class)
    public ProblemDetail handlePrerequisite(MaintenancePrerequisiteException ex) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                CODE_PREREQUISITE,
                "Maintenance prerequisite not met",
                ex.getMessage()
        );
    }

    @ExceptionHandler(MaintenanceSupervisorRequiredException.class)
    public ProblemDetail handleSupervisor(MaintenanceSupervisorRequiredException ex) {
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "Forbidden", ex.getMessage());
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ProblemDetail handleInvalidRequest(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, CODE_INVALID_REQUEST, "Invalid request", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        if (ex instanceof org.springframework.security.access.AccessDeniedException) {
            return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "Forbidden", "You are not allowed to perform this action.");
        }
        if (ex instanceof org.springframework.security.core.AuthenticationException) {
            return problem(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Unauthorized", "Authentication is required.");
        }
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                CODE_INTERNAL_ERROR,
                "Unexpected error",
                "An unexpected error occurred"
        );
    }

    private static ProblemDetail problem(HttpStatus status, String code, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("code", code);
        return problem;
    }
}
