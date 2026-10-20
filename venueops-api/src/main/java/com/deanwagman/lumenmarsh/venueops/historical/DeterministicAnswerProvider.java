package com.deanwagman.lumenmarsh.venueops.historical;

import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AnalyticsAnswer;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AnalyticsQuestion;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AnswerClaim;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AttractionAvailability;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AvailabilitySummary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeterministicAnswerProvider implements AnswerProvider {

    @Override
    public AnalyticsAnswer answer(AnalyticsQuestion question, AvailabilitySummary summary) {
        if (question == null || question.question() == null || question.question().isBlank()) {
            throw new IllegalArgumentException("question is required");
        }
        AttractionAvailability worst = summary.attractions().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No scheduled attraction service in the selected range"));
        String claimText = "%s had the worst uptime at %s%% (%d of %d scheduled minutes operating)."
                .formatted(worst.attractionName(), worst.uptimePercentage(), worst.operatingMinutes(),
                        worst.scheduledMinutes());
        String answer = claimText + " Longest outage: " + worst.longestOutageMinutes() + " minutes.";
        return new AnalyticsAnswer(summary.datasetId(), answer,
                List.of(new AnswerClaim(claimText, worst.citations())), "deterministic-curated-availability");
    }
}
