package com.deanwagman.lumenmarsh.venueops.dashboard.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import com.deanwagman.lumenmarsh.venueops.dashboard.api.OperatorDashboardResponse;
import com.deanwagman.lumenmarsh.venueops.dashboard.domain.DashboardAttentionKind;
import com.deanwagman.lumenmarsh.venueops.dashboard.domain.DashboardFreshnessStatus;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendation;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationId;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationInbound;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorDashboardAssemblerTest {

    private static final Instant NOW = Instant.parse("2026-09-10T20:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void summaryCountsExcludeResolvedIncidentsAndSeedClosedRides() {
        Attraction operating = attraction("mangrove-run", "Mangrove Run", AttractionStatus.OPERATING, CapacityMode.NORMAL, 0);
        Attraction hold = attraction("cypress-coil", "Cypress Coil", AttractionStatus.WEATHER_HOLD, CapacityMode.NOT_APPLICABLE, 1);
        Attraction seedClosed = attraction("stormglass-station", "Stormglass Station", AttractionStatus.CLOSED, CapacityMode.NOT_APPLICABLE, 0);

        Incident openMajor = Incident.report(
                new IncidentId("inc-major"),
                "Lightning near western basin",
                IncidentType.WEATHER,
                IncidentSeverity.MAJOR,
                "INTERNAL ONLY",
                List.of(new AttractionId("cypress-coil")),
                "Operator One",
                CLOCK
        );
        Incident resolved = Incident.report(
                new IncidentId("inc-resolved"),
                "Resolved medical",
                IncidentType.MEDICAL,
                IncidentSeverity.MINOR,
                "gone",
                List.of(),
                "Operator One",
                CLOCK
        );
        resolved.acknowledge("Operator One", null, CLOCK);
        resolved.startMitigation("Operator One", null, CLOCK);
        resolved.resolve("Operator One", "cleared", CLOCK);

        WeatherRecommendation pending = WeatherRecommendation.receive(inbound("rec-1", WeatherRecommendationSeverity.WARNING), CLOCK);

        OperatorDashboardResponse dashboard = OperatorDashboardAssembler.assemble(
                NOW,
                List.of(operating, hold, seedClosed),
                List.of(openMajor, resolved),
                List.of(pending)
        );

        assertThat(dashboard.summary().operatingAttractions()).isEqualTo(1);
        assertThat(dashboard.summary().attractionsNeedingAttention()).isEqualTo(1);
        assertThat(dashboard.summary().closedAttractions()).isEqualTo(1);
        assertThat(dashboard.summary().weatherHoldAttractions()).isEqualTo(1);
        assertThat(dashboard.summary().openIncidents()).isEqualTo(1);
        assertThat(dashboard.summary().majorOrCriticalIncidents()).isEqualTo(1);
        assertThat(dashboard.summary().pendingWeatherRecommendations()).isEqualTo(1);
        assertThat(dashboard.openIncidents()).extracting(OperatorDashboardResponse.DashboardIncidentResponse::id)
                .containsExactly("inc-major");
        assertThat(dashboard.attractionsNeedingAttention()).extracting(OperatorDashboardResponse.DashboardAttractionResponse::id)
                .containsExactly("cypress-coil");
        assertThat(dashboard.attractionsNeedingAttention().getFirst().relatedOpenIncidentCount()).isEqualTo(1);
        assertThat(dashboard.publishedGuestAdvisories()).isEmpty();
        assertThat(dashboard.freshness().venueOps().status()).isEqualTo(DashboardFreshnessStatus.LIVE);
    }

    @Test
    void attentionOrderIsCriticalThenWeatherThenHolds() {
        Attraction hold = attraction("cypress-coil", "Cypress Coil", AttractionStatus.WEATHER_HOLD, CapacityMode.NOT_APPLICABLE, 1);
        Incident critical = Incident.report(
                new IncidentId("inc-critical"),
                "Guest medical emergency",
                IncidentType.MEDICAL,
                IncidentSeverity.CRITICAL,
                "internal",
                List.of(),
                "Operator One",
                CLOCK
        );
        Incident major = Incident.report(
                new IncidentId("inc-major"),
                "Lightning near western basin",
                IncidentType.WEATHER,
                IncidentSeverity.MAJOR,
                "internal",
                List.of(),
                "Operator One",
                CLOCK
        );
        WeatherRecommendation pending = WeatherRecommendation.receive(inbound("rec-warn", WeatherRecommendationSeverity.WARNING), CLOCK);

        OperatorDashboardResponse dashboard = OperatorDashboardAssembler.assemble(
                NOW,
                List.of(hold),
                List.of(major, critical),
                List.of(pending)
        );

        assertThat(dashboard.needsAttention())
                .extracting(OperatorDashboardResponse.DashboardAttentionItemResponse::kind)
                .containsExactly(
                        DashboardAttentionKind.CRITICAL_INCIDENT,
                        DashboardAttentionKind.MAJOR_INCIDENT,
                        DashboardAttentionKind.WEATHER_HAZARD,
                        DashboardAttentionKind.WEATHER_HOLD
                );
        assertThat(dashboard.openIncidents())
                .extracting(OperatorDashboardResponse.DashboardIncidentResponse::id)
                .containsExactly("inc-critical", "inc-major");
    }

    @Test
    void publishedAdvisoryOmitsInternalDescriptionAndGuestFeedShowsGuestCopy() {
        Incident incident = Incident.report(
                new IncidentId("inc-adv"),
                "Lightning near western basin",
                IncidentType.WEATHER,
                IncidentSeverity.MAJOR,
                "INTERNAL ONLY — must never reach guests",
                List.of(new AttractionId("mangrove-run")),
                "Operator One",
                CLOCK
        );
        incident.publishGuestAdvisory(
                "Outdoor weather pause",
                "Some outdoor attractions are temporarily paused.",
                "Supervisor One",
                null,
                CLOCK
        );

        OperatorDashboardResponse dashboard = OperatorDashboardAssembler.assemble(
                NOW,
                List.of(),
                List.of(incident),
                List.of()
        );

        assertThat(dashboard.summary().publishedGuestAdvisories()).isEqualTo(1);
        assertThat(dashboard.publishedGuestAdvisories()).hasSize(1);
        assertThat(dashboard.publishedGuestAdvisories().getFirst().title()).isEqualTo("Outdoor weather pause");
        assertThat(dashboard.publishedGuestAdvisories().getFirst().message())
                .isEqualTo("Some outdoor attractions are temporarily paused.");
        assertThat(dashboard.publishedGuestAdvisories().getFirst().incidentId()).isEqualTo("inc-adv");
        assertThat(dashboard.publishedGuestAdvisories().getFirst().toString()).doesNotContain("INTERNAL ONLY");
    }

    @Test
    void recentActivityIsNewestFirstAndIncludesIncidentActors() {
        Incident incident = Incident.report(
                new IncidentId("inc-activity"),
                "Lightning near western basin",
                IncidentType.WEATHER,
                IncidentSeverity.MODERATE,
                "internal",
                List.of(),
                "Operator One",
                CLOCK
        );

        OperatorDashboardResponse dashboard = OperatorDashboardAssembler.assemble(
                NOW,
                List.of(),
                List.of(incident),
                List.of()
        );

        assertThat(dashboard.recentActivity()).isNotEmpty();
        assertThat(dashboard.recentActivity().getFirst().actor()).isEqualTo("Operator One");
        assertThat(dashboard.recentActivity().getFirst().domain()).isEqualTo("INCIDENT");
        assertThat(dashboard.recentActivity().getFirst().resultingVersion()).isEqualTo(1);
        assertThat(dashboard.recentActivity()).allSatisfy(event ->
                assertThat(event.actor()).isNotEqualTo("spoofed-attacker"));
    }

    @Test
    void freshnessIsStaleWhenLastUpdateIsOlderThanThreshold() {
        Instant generatedAt = NOW.plus(OperatorDashboardAssembler.STALE_AFTER).plusSeconds(1);
        Attraction attraction = attraction("mangrove-run", "Mangrove Run", AttractionStatus.OPERATING, CapacityMode.NORMAL, 0);

        OperatorDashboardResponse dashboard = OperatorDashboardAssembler.assemble(
                generatedAt,
                List.of(attraction),
                List.of(),
                List.of()
        );

        assertThat(dashboard.freshness().venueOps().status()).isEqualTo(DashboardFreshnessStatus.STALE);
        assertThat(dashboard.needsAttention())
                .extracting(OperatorDashboardResponse.DashboardAttentionItemResponse::kind)
                .contains(DashboardAttentionKind.STALE_DATA);
    }

    @Test
    void environmentalFreshnessIsUnavailableWithoutWeatherData() {
        OperatorDashboardResponse dashboard = OperatorDashboardAssembler.assemble(NOW, List.of(), List.of(), List.of());

        assertThat(dashboard.freshness().environmentalData().status()).isEqualTo(DashboardFreshnessStatus.UNAVAILABLE);
        assertThat(dashboard.summary().operatingAttractions()).isZero();
    }

    private static Attraction attraction(
            String id,
            String name,
            AttractionStatus status,
            CapacityMode capacity,
            long version
    ) {
        return Attraction.rehydrate(
                new AttractionId(id),
                name,
                "Western Basin",
                AttractionType.BOAT_EXPEDITION,
                status,
                capacity,
                status == AttractionStatus.OPERATING ? 12 : null,
                NOW,
                version,
                List.of()
        );
    }

    private static WeatherRecommendationInbound inbound(String id, WeatherRecommendationSeverity severity) {
        return new WeatherRecommendationInbound(
                new WeatherRecommendationId(id),
                "simulated-lightning-hold",
                WeatherRecommendationStatus.ACTIVE,
                severity,
                "Place outdoor attractions on weather hold",
                "Simulated lightning strike near the western basin.",
                "Place Mangrove Run and Cypress Coil on weather hold",
                List.of(new AttractionId("cypress-coil")),
                NOW,
                1
        );
    }
}
