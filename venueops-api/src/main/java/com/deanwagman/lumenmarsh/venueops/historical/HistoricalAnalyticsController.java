package com.deanwagman.lumenmarsh.venueops.historical;

import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AnalyticsAnswer;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AnalyticsQuestion;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AvailabilityDetail;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AvailabilitySummary;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Dataset;
import com.deanwagman.lumenmarsh.venueops.security.VenueOpsScopes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/operator/analytics")
@PreAuthorize("hasAuthority('" + VenueOpsScopes.SCOPE_OPERATOR_READ + "')")
public class HistoricalAnalyticsController {

    private final HistoricalStore store;
    private final AvailabilityProjectionService availability;
    private final HistoricalReplayService replay;
    private final AnswerProvider answerProvider;

    public HistoricalAnalyticsController(HistoricalStore store, AvailabilityProjectionService availability,
                                         HistoricalReplayService replay, AnswerProvider answerProvider) {
        this.store = store;
        this.availability = availability;
        this.replay = replay;
        this.answerProvider = answerProvider;
    }

    @GetMapping("/datasets")
    public List<Dataset> datasets() {
        return store.listDatasets();
    }

    @GetMapping("/availability")
    public AvailabilitySummary availability(
            @RequestParam @NotBlank String datasetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @NotNull LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @NotNull LocalDate to
    ) {
        return availability.summary(datasetId, from, to);
    }

    @GetMapping("/availability/attractions/{attractionId}")
    public AvailabilityDetail detail(
            @PathVariable String attractionId,
            @RequestParam @NotBlank String datasetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @NotNull LocalDate date
    ) {
        return availability.detail(datasetId, attractionId, date);
    }

    @PostMapping("/questions")
    public AnalyticsAnswer question(@Valid @RequestBody QuestionRequest request) {
        AvailabilitySummary curated = availability.summary(request.datasetId(), request.from(), request.to());
        return answerProvider.answer(request.toDomain(), curated);
    }

    @PostMapping("/datasets/{datasetId}/replay")
    @PreAuthorize("hasAuthority('" + VenueOpsScopes.ROLE_SUPERVISOR + "')")
    public HistoricalReplayService.ReplayResult replay(
            @PathVariable String datasetId,
            @RequestParam(defaultValue = "false") boolean replace
    ) {
        return replay.replayFixture(datasetId, replace);
    }

    public record QuestionRequest(
            @NotBlank String datasetId,
            @NotNull LocalDate from,
            @NotNull LocalDate to,
            @NotBlank String question
    ) {
        AnalyticsQuestion toDomain() {
            return new AnalyticsQuestion(datasetId, from, to, question);
        }
    }
}
