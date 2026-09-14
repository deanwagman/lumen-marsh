package com.deanwagman.lumenmarsh.venueops.weather.application;

import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentNotFoundException;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentRepository;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.weather.domain.InboundApplyResult;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendation;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationActivity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationCommand;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationId;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationInbound;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class WeatherRecommendationService {

    private final WeatherRecommendationRepository recommendations;
    private final IncidentRepository incidents;
    private final Clock clock;
    private final WeatherRecommendationUpdatePublisher publisher;

    public WeatherRecommendationService(
            WeatherRecommendationRepository recommendations,
            IncidentRepository incidents,
            Clock clock,
            WeatherRecommendationUpdatePublisher publisher
    ) {
        this.recommendations = Objects.requireNonNull(recommendations);
        this.incidents = Objects.requireNonNull(incidents);
        this.clock = Objects.requireNonNull(clock);
        this.publisher = Objects.requireNonNull(publisher);
    }

    public List<WeatherRecommendation> list() {
        return recommendations.findAll().stream()
                .sorted(Comparator
                        .comparing((WeatherRecommendation recommendation) -> recommendation.status().name())
                        .thenComparing(WeatherRecommendation::updatedAt, Comparator.reverseOrder()))
                .toList();
    }

    public WeatherRecommendation get(WeatherRecommendationId id) {
        return recommendations.findById(id).orElseThrow(() -> new WeatherRecommendationNotFoundException(id));
    }

    public WeatherRecommendationIngestResult ingest(WeatherRecommendationInbound inbound) {
        return recommendations.findById(inbound.id())
                .map(existing -> applyInbound(existing, inbound))
                .orElseGet(() -> create(inbound));
    }

    public WeatherRecommendation execute(
            WeatherRecommendationId id,
            WeatherRecommendationCommand command,
            String actor,
            String reason,
            long expectedVersion,
            String incidentId
    ) {
        WeatherRecommendation recommendation = get(id);
        if (recommendation.version() != expectedVersion) {
            throw new StaleWeatherRecommendationVersionException(id, expectedVersion, recommendation.version());
        }
        apply(recommendation, command, actor, reason, incidentId);
        WeatherRecommendationActivity activity = lastUncommitted(recommendation);
        recommendations.save(recommendation);
        publisher.publish(WeatherRecommendationOperationalUpdate.fromActivity(activity, recommendation));
        return recommendation;
    }

    private WeatherRecommendationIngestResult create(WeatherRecommendationInbound inbound) {
        WeatherRecommendation recommendation = WeatherRecommendation.receive(inbound, clock);
        WeatherRecommendationActivity activity = lastUncommitted(recommendation);
        recommendations.save(recommendation);
        publisher.publish(WeatherRecommendationOperationalUpdate.fromActivity(activity, recommendation));
        return WeatherRecommendationIngestResult.created(recommendation);
    }

    private WeatherRecommendationIngestResult applyInbound(
            WeatherRecommendation existing,
            WeatherRecommendationInbound inbound
    ) {
        InboundApplyResult result = existing.applyInbound(inbound, clock);
        if (result == InboundApplyResult.DUPLICATE) {
            return WeatherRecommendationIngestResult.duplicate(existing);
        }
        WeatherRecommendationActivity activity = lastUncommitted(existing);
        recommendations.save(existing);
        publisher.publish(WeatherRecommendationOperationalUpdate.fromActivity(activity, existing));
        return WeatherRecommendationIngestResult.updated(existing);
    }

    private void apply(
            WeatherRecommendation recommendation,
            WeatherRecommendationCommand command,
            String actor,
            String reason,
            String incidentId
    ) {
        switch (command) {
            case ACKNOWLEDGE -> recommendation.acknowledge(actor, reason, clock);
            case DISMISS -> recommendation.dismiss(actor, reason, clock);
            case LINK_INCIDENT -> {
                IncidentId linked = new IncidentId(requireCommandValue(incidentId, "LINK_INCIDENT requires incidentId"));
                if (incidents.findById(linked).isEmpty()) {
                    throw new IncidentNotFoundException(linked);
                }
                recommendation.linkIncident(linked, actor, reason, clock);
            }
        }
    }

    private static WeatherRecommendationActivity lastUncommitted(WeatherRecommendation recommendation) {
        List<WeatherRecommendationActivity> uncommitted = recommendation.uncommittedActivity();
        if (uncommitted.isEmpty()) {
            throw new IllegalStateException("Accepted command produced no activity");
        }
        return uncommitted.getLast();
    }

    private static String requireCommandValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
