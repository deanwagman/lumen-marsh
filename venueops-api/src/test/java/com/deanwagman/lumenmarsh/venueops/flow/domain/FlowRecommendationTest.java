package com.deanwagman.lumenmarsh.venueops.flow.domain;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.security.ActorIdentity;
import com.deanwagman.lumenmarsh.venueops.security.ActorType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowRecommendationTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-15T18:00:00Z"), ZoneOffset.UTC);
    private static final ActorIdentity OPERATOR = new ActorIdentity(
            "operator-sub-1",
            "Operator One",
            ActorType.HUMAN,
            "http://venueops.local/test"
    );
    private static final ActorIdentity SUPERVISOR = new ActorIdentity(
            "supervisor-sub-1",
            "Supervisor One",
            ActorType.HUMAN,
            "http://venueops.local/test"
    );

    @Test
    void approvePublishWithdraw() {
        FlowRecommendation recommendation = pending();
        recommendation.apply(
                FlowRecommendationCommand.APPROVE,
                OPERATOR,
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                "corr",
                null,
                null,
                CLOCK
        );
        assertThat(recommendation.status()).isEqualTo(FlowRecommendationStatus.APPROVED);

        recommendation.apply(
                FlowRecommendationCommand.PUBLISH,
                SUPERVISOR,
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                "corr",
                "Mangrove Run remains unavailable.",
                "Lantern Ferry currently has a shorter wait.",
                CLOCK
        );
        assertThat(recommendation.status()).isEqualTo(FlowRecommendationStatus.PUBLISHED);
        assertThat(recommendation.guestMessage()).contains("Lantern Ferry");

        recommendation.apply(
                FlowRecommendationCommand.WITHDRAW,
                SUPERVISOR,
                UUID.fromString("33333333-3333-4333-8333-333333333333"),
                "corr",
                "Capacity restored.",
                null,
                CLOCK
        );
        assertThat(recommendation.status()).isEqualTo(FlowRecommendationStatus.WITHDRAWN);
    }

    @Test
    void cannotPublishFromPending() {
        FlowRecommendation recommendation = pending();
        assertThatThrownBy(() -> recommendation.apply(
                FlowRecommendationCommand.PUBLISH,
                SUPERVISOR,
                UUID.randomUUID(),
                "corr",
                "reason",
                "message",
                CLOCK
        )).isInstanceOf(InvalidFlowRecommendationTransitionException.class);
    }

    @Test
    void dismissRequiresReason() {
        FlowRecommendation recommendation = pending();
        assertThatThrownBy(() -> recommendation.apply(
                FlowRecommendationCommand.DISMISS,
                OPERATOR,
                UUID.randomUUID(),
                "corr",
                " ",
                null,
                CLOCK
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private static FlowRecommendation pending() {
        return FlowRecommendation.create(
                new FlowRecommendationId("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
                FlowRecommendationType.CONGESTION_EXPECTED,
                FlowRecommendationSeverity.WARNING,
                new AttractionId("mangrove-run"),
                List.of(new AttractionId("mangrove-run"), new AttractionId("cypress-coil")),
                List.of(new AttractionId("cypress-coil")),
                "Congestion expected at Cypress Coil",
                "Mangrove Run capacity dropped; arrivals are shifting.",
                null,
                Instant.parse("2026-09-15T20:00:00Z"),
                null,
                null,
                true,
                OPERATOR,
                UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
                "corr-create",
                CLOCK
        );
    }
}
