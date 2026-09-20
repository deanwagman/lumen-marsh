package com.deanwagman.lumenmarsh.venueops.flow.api;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionNotFoundException;
import com.deanwagman.lumenmarsh.venueops.flow.application.ConflictingFlowCommandException;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowObservationNotFoundException;
import com.deanwagman.lumenmarsh.venueops.flow.application.FlowRecommendationNotFoundException;
import com.deanwagman.lumenmarsh.venueops.flow.application.StaleFlowRecommendationVersionException;
import com.deanwagman.lumenmarsh.venueops.flow.domain.InvalidFlowObservationException;
import com.deanwagman.lumenmarsh.venueops.flow.domain.InvalidFlowRecommendationTransitionException;
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

@RestControllerAdvice(basePackages = "com.deanwagman.lumenmarsh.venueops.flow")
public class FlowExceptionHandler {

    public static final String CODE_NOT_FOUND = "FLOW_NOT_FOUND";
    public static final String CODE_INVALID_REQUEST = "INVALID_REQUEST";
    public static final String CODE_INVALID_TRANSITION = "INVALID_TRANSITION";
    public static final String CODE_STALE_VERSION = "STALE_VERSION";
    public static final String CODE_DUPLICATE_COMMAND = "DUPLICATE_COMMAND";
    public static final String CODE_INTERNAL_ERROR = "INTERNAL_ERROR";

    @ExceptionHandler({
            FlowRecommendationNotFoundException.class,
            FlowObservationNotFoundException.class
    })
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return problem(HttpStatus.NOT_FOUND, CODE_NOT_FOUND, "Flow resource not found", ex.getMessage());
    }

    @ExceptionHandler(AttractionNotFoundException.class)
    public ProblemDetail handleAttractionNotFound(AttractionNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "ATTRACTION_NOT_FOUND", "Attraction not found", ex.getMessage());
    }

    @ExceptionHandler(InvalidFlowRecommendationTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidFlowRecommendationTransitionException ex) {
        ProblemDetail detail = problem(
                HttpStatus.CONFLICT,
                CODE_INVALID_TRANSITION,
                "Invalid flow recommendation transition",
                ex.getMessage()
        );
        detail.setProperty("currentStatus", ex.currentStatus().name());
        detail.setProperty("command", ex.command().name());
        return detail;
    }

    @ExceptionHandler(StaleFlowRecommendationVersionException.class)
    public ProblemDetail handleStaleVersion(StaleFlowRecommendationVersionException ex) {
        ProblemDetail detail = problem(
                HttpStatus.CONFLICT,
                CODE_STALE_VERSION,
                "Stale flow recommendation version",
                ex.getMessage()
        );
        detail.setType(URI.create("https://lumen-marsh.dev/problems/stale-version"));
        detail.setProperty("currentVersion", ex.actualVersion());
        detail.setProperty("expectedVersion", ex.expectedVersion());
        return detail;
    }

    @ExceptionHandler(ConflictingFlowCommandException.class)
    public ProblemDetail handleDuplicateCommand(ConflictingFlowCommandException ex) {
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

    @ExceptionHandler({
            InvalidFlowObservationException.class,
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
