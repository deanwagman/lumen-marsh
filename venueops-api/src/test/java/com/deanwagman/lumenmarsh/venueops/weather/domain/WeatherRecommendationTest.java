package com.deanwagman.lumenmarsh.venueops.weather.domain;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeatherRecommendationTest {

    private static final Instant NOW = Instant.parse("2026-09-01T16:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final WeatherRecommendationId ID = new WeatherRecommendationId("rec-lightning-1");
    private static final AttractionId MANGROVE_RUN = new AttractionId("mangrove-run");
    private static final AttractionId CYPRESS_COIL = new AttractionId("cypress-coil");

    @Test
    void receiveCreatesPendingActiveRecommendationAtVersionOne() {
        WeatherRecommendation recommendation = received();

        assertThat(recommendation.status()).isEqualTo(WeatherRecommendationStatus.ACTIVE);
        assertThat(recommendation.operatorStatus()).isEqualTo(WeatherRecommendationOperatorStatus.PENDING);
        assertThat(recommendation.version()).isEqualTo(1);
        assertThat(recommendation.sourceVersion()).isEqualTo(1);
        assertThat(recommendation.receivedAt()).isEqualTo(NOW);
        assertThat(recommendation.observedAt()).isEqualTo(NOW);
        assertThat(recommendation.affectedAttractionIds()).containsExactly(MANGROVE_RUN, CYPRESS_COIL);
        assertThat(recommendation.simulated()).isTrue();
        assertThat(recommendation.activity()).hasSize(1);
        assertThat(recommendation.activity().getFirst().type()).isEqualTo(WeatherRecommendationEventType.RECEIVED);
        assertThat(recommendation.activity().getFirst().actor()).isEqualTo(WeatherRecommendation.SYSTEM_ACTOR);
        assertThat(recommendation.uncommittedActivity()).hasSize(1);
    }

    @Test
    void sameSourceVersionIsDuplicateWithoutActivity() {
        WeatherRecommendation recommendation = received();
        recommendation.markActivityCommitted();

        InboundApplyResult result = recommendation.applyInbound(activeInbound(1), CLOCK);

        assertThat(result).isEqualTo(InboundApplyResult.DUPLICATE);
        assertThat(recommendation.version()).isEqualTo(1);
        assertThat(recommendation.uncommittedActivity()).isEmpty();
        assertThat(recommendation.activity()).hasSize(1);
    }

    @Test
    void newerSourceVersionUpdatesFieldsAndIncrementsInboxVersion() {
        WeatherRecommendation recommendation = received();
        recommendation.markActivityCommitted();

        InboundApplyResult result = recommendation.applyInbound(clearedInbound(2), CLOCK);

        assertThat(result).isEqualTo(InboundApplyResult.UPDATED);
        assertThat(recommendation.status()).isEqualTo(WeatherRecommendationStatus.CLEARED);
        assertThat(recommendation.sourceVersion()).isEqualTo(2);
        assertThat(recommendation.version()).isEqualTo(2);
        assertThat(recommendation.operatorStatus()).isEqualTo(WeatherRecommendationOperatorStatus.PENDING);
        assertThat(recommendation.receivedAt()).isEqualTo(NOW);
        assertThat(recommendation.uncommittedActivity()).hasSize(1);
        assertThat(recommendation.uncommittedActivity().getFirst().type())
                .isEqualTo(WeatherRecommendationEventType.SOURCE_UPDATED);
    }

    @Test
    void olderSourceVersionIsRejectedAsStale() {
        WeatherRecommendation recommendation = received();
        recommendation.applyInbound(clearedInbound(2), CLOCK);

        assertThatThrownBy(() -> recommendation.applyInbound(activeInbound(1), CLOCK))
                .isInstanceOf(StaleWeatherRecommendationSourceException.class)
                .hasMessageContaining("older than stored 2");
        assertThat(recommendation.sourceVersion()).isEqualTo(2);
    }

    @Test
    void operatorCommandsDoNotChangeSourceStatus() {
        WeatherRecommendation recommendation = received();

        recommendation.acknowledge("Operator One", null, CLOCK);
        recommendation.linkIncident(new IncidentId("inc-1"), "Operator One", "Opened weather incident", CLOCK);

        assertThat(recommendation.operatorStatus()).isEqualTo(WeatherRecommendationOperatorStatus.LINKED);
        assertThat(recommendation.linkedIncidentId()).isEqualTo(new IncidentId("inc-1"));
        assertThat(recommendation.status()).isEqualTo(WeatherRecommendationStatus.ACTIVE);
        assertThat(recommendation.version()).isEqualTo(3);
    }

    @Test
    void dismissRejectsFurtherLinking() {
        WeatherRecommendation recommendation = received();
        recommendation.dismiss("Operator One", "False alarm", CLOCK);

        assertThatThrownBy(() -> recommendation.linkIncident(new IncidentId("inc-1"), "Operator One", null, CLOCK))
                .isInstanceOf(InvalidWeatherRecommendationTransitionException.class);
        assertThat(recommendation.operatorStatus()).isEqualTo(WeatherRecommendationOperatorStatus.DISMISSED);
    }

    @Test
    void realObservationIsNotMarkedSimulated() {
        WeatherRecommendationInbound inbound = new WeatherRecommendationInbound(
                ID,
                "nws-lightning-hold",
                WeatherRecommendationStatus.ACTIVE,
                WeatherRecommendationSeverity.WARNING,
                "Lightning nearby",
                "NWS observation within 5 miles.",
                "Place outdoor attractions on weather hold",
                List.of(MANGROVE_RUN),
                NOW,
                1L
        );

        assertThat(WeatherRecommendation.receive(inbound, CLOCK).simulated()).isFalse();
    }

    private static WeatherRecommendation received() {
        return WeatherRecommendation.receive(activeInbound(1), CLOCK);
    }

    private static WeatherRecommendationInbound activeInbound(long sourceVersion) {
        return new WeatherRecommendationInbound(
                ID,
                "simulated-lightning-hold",
                WeatherRecommendationStatus.ACTIVE,
                WeatherRecommendationSeverity.WARNING,
                "Place Mangrove Run and Cypress Coil on weather hold",
                "Simulated lightning strike 1.2 miles from the western basin.",
                "Place Mangrove Run and Cypress Coil on weather hold",
                List.of(MANGROVE_RUN, CYPRESS_COIL),
                NOW,
                sourceVersion
        );
    }

    private static WeatherRecommendationInbound clearedInbound(long sourceVersion) {
        return new WeatherRecommendationInbound(
                ID,
                "simulated-lightning-hold",
                WeatherRecommendationStatus.CLEARED,
                WeatherRecommendationSeverity.INFO,
                "Lightning hold can be reviewed for clearance",
                "Simulated lightning is outside the hold radius.",
                "Review weather holds for return-to-service",
                List.of(MANGROVE_RUN, CYPRESS_COIL),
                NOW,
                sourceVersion
        );
    }
}
