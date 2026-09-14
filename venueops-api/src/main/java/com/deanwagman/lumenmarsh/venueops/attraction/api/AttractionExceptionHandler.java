package com.deanwagman.lumenmarsh.venueops.attraction.api;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionNotFoundException;
import com.deanwagman.lumenmarsh.venueops.attraction.application.StaleAttractionVersionException;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.InvalidAttractionTransitionException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackages = "com.deanwagman.lumenmarsh.venueops.attraction")
public class AttractionExceptionHandler {

    public static final String CODE_ATTRACTION_NOT_FOUND = "ATTRACTION_NOT_FOUND";
    public static final String CODE_INVALID_REQUEST = "INVALID_REQUEST";
    public static final String CODE_INVALID_TRANSITION = "INVALID_TRANSITION";
    public static final String CODE_STALE_VERSION = "STALE_VERSION";
    public static final String CODE_INTERNAL_ERROR = "INTERNAL_ERROR";

    @ExceptionHandler(AttractionNotFoundException.class)
    public ProblemDetail handleNotFound(AttractionNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, CODE_ATTRACTION_NOT_FOUND, "Attraction not found", ex.getMessage());
    }

    @ExceptionHandler(InvalidAttractionTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidAttractionTransitionException ex) {
        ProblemDetail detail = problem(
                HttpStatus.CONFLICT,
                CODE_INVALID_TRANSITION,
                "Invalid attraction transition",
                ex.getMessage()
        );
        detail.setProperty("currentStatus", ex.currentStatus().name());
        detail.setProperty("command", ex.command().name());
        return detail;
    }

    @ExceptionHandler(StaleAttractionVersionException.class)
    public ProblemDetail handleStaleVersion(StaleAttractionVersionException ex) {
        ProblemDetail detail = problem(
                HttpStatus.CONFLICT,
                CODE_STALE_VERSION,
                "Stale attraction version",
                ex.getMessage()
        );
        detail.setProperty("expectedVersion", ex.expectedVersion());
        detail.setProperty("actualVersion", ex.actualVersion());
        return detail;
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
