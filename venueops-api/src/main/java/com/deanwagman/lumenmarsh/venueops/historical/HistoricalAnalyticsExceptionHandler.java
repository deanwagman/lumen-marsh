package com.deanwagman.lumenmarsh.venueops.historical;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDate;

@RestControllerAdvice(assignableTypes = HistoricalAnalyticsController.class)
public class HistoricalAnalyticsExceptionHandler {

    @ExceptionHandler({HistoricalDatasetNotFoundException.class, HistoricalPlanNotFoundException.class})
    ProblemDetail notFound(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Historical analytics data not found");
        problem.setProperty("code", "HISTORICAL_DATA_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ProblemDetail invalid(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid historical analytics request");
        problem.setProperty("code", "INVALID_HISTORICAL_REQUEST");
        return problem;
    }
}

final class HistoricalDatasetNotFoundException extends RuntimeException {
    HistoricalDatasetNotFoundException(String datasetId) {
        super("Historical dataset not found: " + datasetId);
    }
}

final class HistoricalPlanNotFoundException extends RuntimeException {
    HistoricalPlanNotFoundException(String datasetId, String attractionId, LocalDate date) {
        super("No attraction plan for dataset %s, attraction %s, date %s"
                .formatted(datasetId, attractionId, date));
    }
}
