package com.deanwagman.lumenmarsh.venueops.historical;

import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AnalyticsAnswer;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AnalyticsQuestion;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AnswerClaim;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AttractionAvailability;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AvailabilityClassification;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AvailabilityDetail;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AvailabilitySegment;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.AvailabilitySummary;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Citation;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.Evidence;
import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels.EvidenceClassification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class DeterministicAnswerProvider implements AnswerProvider {

    static final String WORST_UPTIME_QUESTION = "Which attraction had the worst uptime?";
    static final String CAUSE_QUESTION = "What caused it?";
    static final String DAY_QUESTION = "Which day?";
    private static final String PROVIDER = "deterministic-curated-availability";

    @Override
    public AnalyticsAnswer answer(AnalyticsQuestion question, AvailabilitySummary summary) {
        String normalized = normalize(requiredQuestion(question));
        return summary.answers().stream()
                .filter(item -> normalize(item.question()).equals(normalized))
                .findFirst()
                .orElseGet(() -> {
                    if (normalize(WORST_UPTIME_QUESTION).equals(normalized)) {
                        return worst(summary);
                    }
                    throw unsupported();
                });
    }

    @Override
    public List<AnalyticsAnswer> curated(AvailabilitySummary summary, List<AvailabilityDetail> details,
                                         List<Evidence> evidence) {
        return List.of(worst(summary), cause(summary, details, evidence), day(summary, details, evidence));
    }

    private static AnalyticsAnswer worst(AvailabilitySummary summary) {
        AttractionAvailability worst = worstAttraction(summary);
        String claimText = "%s had the worst uptime at %s%% (%d of %d scheduled minutes operating)."
                .formatted(worst.attractionName(), worst.uptimePercentage(), worst.operatingMinutes(),
                        worst.scheduledMinutes());
        String answer = claimText + " Longest outage: " + worst.longestOutageMinutes() + " minutes.";
        return new AnalyticsAnswer(summary.datasetId(), WORST_UPTIME_QUESTION, answer,
                List.of(new AnswerClaim(claimText, worst.citations())), PROVIDER);
    }

    private static AnalyticsAnswer cause(AvailabilitySummary summary, List<AvailabilityDetail> details,
                                         List<Evidence> evidence) {
        AttractionAvailability worst = worstAttraction(summary);
        AvailabilityDetail outage = outageDay(worst, details);
        List<AvailabilitySegment> downtime = downtimeSegments(outage);
        List<Evidence> episode = episode(worst, outage, evidence);
        String kind = episodeKind(episode);
        String narrative = episode.stream().map(Evidence::summary).reduce((left, right) -> left + " " + right)
                .orElseGet(() -> downtime.isEmpty()
                        ? "No downtime segment explains the ranking."
                        : "Downtime classifications: " + downtime.stream()
                        .map(segment -> segment.classification().name())
                        .distinct()
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("none") + ".");
        String claimText = "%s’s worst uptime was caused by one %s on %s."
                .formatted(worst.attractionName(), kind, outage.parkDate());
        String answer = claimText + " " + narrative;
        return new AnalyticsAnswer(summary.datasetId(), CAUSE_QUESTION, answer,
                List.of(new AnswerClaim(claimText, cited(downtime, episode, worst.attractionId(), outage))), PROVIDER);
    }

    private static AnalyticsAnswer day(AvailabilitySummary summary, List<AvailabilityDetail> details,
                                       List<Evidence> evidence) {
        AttractionAvailability worst = worstAttraction(summary);
        AvailabilityDetail outage = outageDay(worst, details);
        List<AvailabilitySegment> downtime = downtimeSegments(outage);
        List<Evidence> episode = episode(worst, outage, evidence);
        String claimText = "%s’s worst uptime was on %s."
                .formatted(worst.attractionName(), outage.parkDate());
        String answer = claimText + " That day accounts for the " + worst.longestOutageMinutes()
                + "-minute outage.";
        return new AnalyticsAnswer(summary.datasetId(), DAY_QUESTION, answer,
                List.of(new AnswerClaim(claimText, cited(downtime, episode, worst.attractionId(), outage))), PROVIDER);
    }

    private static List<Citation> cited(List<AvailabilitySegment> downtime, List<Evidence> episode,
                                        String attractionId, AvailabilityDetail outage) {
        List<Citation> citations = new ArrayList<>();
        for (AvailabilitySegment segment : downtime) {
            citations.add(segmentCitation(segment, attractionId, outage));
        }
        for (Evidence item : episode) {
            AvailabilitySegment segment = outage.segments().stream()
                    .filter(candidate -> candidate.activityId() != null
                            && item.segmentActivityIds().contains(candidate.activityId()))
                    .findFirst()
                    .orElse(downtime.isEmpty() ? null : downtime.getFirst());
            if (segment == null) {
                continue;
            }
            Citation citedSegment = segmentCitation(segment, attractionId, outage);
            citations.add(new Citation(item.id(), citedSegment.datasetId(), citedSegment.attractionId(),
                    citedSegment.parkDate(), citedSegment.activityId(), citedSegment.commandId(),
                    citedSegment.controlTowerUrl(), item.id()));
        }
        return List.copyOf(citations);
    }

    private static Citation segmentCitation(AvailabilitySegment segment, String attractionId,
                                            AvailabilityDetail outage) {
        return new Citation(segment.citationId(), segment.datasetId(), attractionId, outage.parkDate(),
                segment.activityId(), segment.commandId(), segment.controlTowerUrl(), null);
    }

    private static List<Evidence> episode(AttractionAvailability worst, AvailabilityDetail outage,
                                          List<Evidence> evidence) {
        Set<String> activityIds = new LinkedHashSet<>();
        outage.segments().forEach(segment -> {
            if (segment.activityId() != null) {
                activityIds.add(segment.activityId());
            }
        });
        return evidence.stream()
                .filter(item -> item.attractionId().equals(worst.attractionId()))
                .filter(item -> item.parkDate().equals(outage.parkDate()))
                .filter(item -> item.segmentActivityIds().stream().anyMatch(activityIds::contains))
                .sorted(Comparator.comparing(Evidence::occurredAt).thenComparing(Evidence::id))
                .toList();
    }

    private static String episodeKind(List<Evidence> episode) {
        boolean maintenance = episode.stream().anyMatch(item -> item.classification() == EvidenceClassification.VIBRATION_RECOMMENDATION
                || item.classification() == EvidenceClassification.ACCEPTED_WORK
                || item.classification() == EvidenceClassification.TECHNICAL_DELAY
                || item.classification() == EvidenceClassification.REPAIR);
        if (maintenance) {
            return "maintenance episode";
        }
        boolean weather = episode.stream().anyMatch(item -> item.classification() == EvidenceClassification.LIGHTNING_DETECTED
                || item.classification() == EvidenceClassification.WEATHER_HOLD
                || item.classification() == EvidenceClassification.WEATHER_ALL_CLEAR);
        if (weather) {
            return "weather episode";
        }
        return "operating episode";
    }

    private static List<AvailabilitySegment> downtimeSegments(AvailabilityDetail detail) {
        return detail.segments().stream().filter(segment -> !isOperating(segment.classification())).toList();
    }

    private static boolean isOperating(AvailabilityClassification classification) {
        return classification == AvailabilityClassification.OPERATING
                || classification == AvailabilityClassification.REDUCED_CAPACITY;
    }

    private static AvailabilityDetail outageDay(AttractionAvailability worst, List<AvailabilityDetail> details) {
        return details.stream()
                .filter(detail -> detail.attractionId().equals(worst.attractionId()))
                .min(Comparator.comparingLong((AvailabilityDetail detail) -> -detail.longestOutageMinutes())
                        .thenComparing(AvailabilityDetail::parkDate))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No scheduled attraction service in the selected range"));
    }

    private static AttractionAvailability worstAttraction(AvailabilitySummary summary) {
        return summary.attractions().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No scheduled attraction service in the selected range"));
    }

    private static String requiredQuestion(AnalyticsQuestion question) {
        if (question == null || question.question() == null || question.question().isBlank()) {
            throw new IllegalArgumentException("question is required");
        }
        return question.question();
    }

    private static String normalize(String question) {
        return question.trim().toLowerCase(Locale.ROOT);
    }

    private static IllegalArgumentException unsupported() {
        return new IllegalArgumentException(
                "Unsupported historical question. Supported questions: "
                        + WORST_UPTIME_QUESTION + " " + CAUSE_QUESTION + " " + DAY_QUESTION);
    }
}
