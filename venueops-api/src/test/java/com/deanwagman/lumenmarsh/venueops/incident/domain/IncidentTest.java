package com.deanwagman.lumenmarsh.venueops.incident.domain;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentTest {

    private static final Instant NOW = Instant.parse("2026-09-01T15:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final AttractionId MANGROVE_RUN = new AttractionId("mangrove-run");
    private static final AttractionId CYPRESS_COIL = new AttractionId("cypress-coil");

    @Test
    void reportStartsAtVersionOneWithAuditedActivity() {
        Incident incident = reported();

        assertThat(incident.status()).isEqualTo(IncidentStatus.REPORTED);
        assertThat(incident.version()).isEqualTo(1);
        assertThat(incident.createdAt()).isEqualTo(NOW);
        assertThat(incident.internalDescription()).isEqualTo("Repeated strikes detected within the hold radius.");
        assertThat(incident.attractionIds()).containsExactly(MANGROVE_RUN, CYPRESS_COIL);
        assertThat(incident.guestAdvisoryPublished()).isFalse();
        assertThat(incident.hasActiveGuestAdvisory()).isFalse();
        assertThat(incident.activity()).hasSize(1);
        assertThat(incident.activity().getFirst()).isInstanceOf(IncidentReported.class);
        assertThat(incident.activity().getFirst().previousVersion()).isZero();
        assertThat(incident.activity().getFirst().resultingVersion()).isEqualTo(1);
        assertThat(incident.activity().getFirst().actor()).isEqualTo("Operator One");
        assertThat(incident.uncommittedActivity()).hasSize(1);
    }

    @Test
    void reportDeduplicatesInitialAttractionLinks() {
        Incident incident = Incident.report(
                "Lightning activity near western basin",
                IncidentType.WEATHER,
                IncidentSeverity.MAJOR,
                "Internal",
                List.of(MANGROVE_RUN, MANGROVE_RUN, CYPRESS_COIL),
                "Operator One",
                CLOCK
        );

        assertThat(incident.attractionIds()).containsExactly(MANGROVE_RUN, CYPRESS_COIL);
        assertThat(incident.version()).isEqualTo(1);
    }

    @Test
    void lifecycleCommandsAdvanceStatusAndIncrementVersionOnce() {
        Incident incident = reported();

        incident.acknowledge("Operator One", null, CLOCK);
        assertThat(incident.status()).isEqualTo(IncidentStatus.ACKNOWLEDGED);
        assertThat(incident.version()).isEqualTo(2);

        incident.startMitigation("Operator One", "Hold radius still active", CLOCK);
        assertThat(incident.status()).isEqualTo(IncidentStatus.MITIGATING);
        assertThat(incident.version()).isEqualTo(3);

        incident.resolve("Operator One", "Storm cell moved out of radius", CLOCK);
        assertThat(incident.status()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(incident.version()).isEqualTo(4);
        assertThat(incident.activity()).hasSize(4);
    }

    @Test
    void cannotSkipAcknowledgeOrMitigation() {
        Incident incident = reported();
        assertThatThrownBy(() -> incident.startMitigation("Operator One", null, CLOCK))
                .isInstanceOf(InvalidIncidentTransitionException.class);
        assertThatThrownBy(() -> incident.resolve("Operator One", "Too soon", CLOCK))
                .isInstanceOf(InvalidIncidentTransitionException.class);
        assertThat(incident.version()).isEqualTo(1);
        assertThat(incident.activity()).hasSize(1);
    }

    @Test
    void resolvedIncidentsCannotBeModified() {
        Incident incident = resolved();

        assertThatThrownBy(() -> incident.assign("Operator One", "Operator One", null, CLOCK))
                .isInstanceOf(InvalidIncidentTransitionException.class);
        assertThatThrownBy(() -> incident.changeSeverity(IncidentSeverity.CRITICAL, "Operator One", "Escalate", CLOCK))
                .isInstanceOf(InvalidIncidentTransitionException.class);
        assertThatThrownBy(() -> incident.linkAttraction(new AttractionId("stormglass-station"), "Operator One", null, CLOCK))
                .isInstanceOf(InvalidIncidentTransitionException.class);
        assertThatThrownBy(() -> incident.publishGuestAdvisory("Title", "Message", "Operator One", null, CLOCK))
                .isInstanceOf(InvalidIncidentTransitionException.class);
        assertThat(incident.version()).isEqualTo(4);
        assertThat(incident.activity()).hasSize(4);
    }

    @Test
    void resolutionRequiresAReason() {
        Incident incident = mitigating();
        assertThatThrownBy(() -> incident.resolve("Operator One", "  ", CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RESOLVE");
        assertThat(incident.status()).isEqualTo(IncidentStatus.MITIGATING);
    }

    @Test
    void everyMutationRequiresAnActor() {
        Incident incident = reported();
        assertThatThrownBy(() -> incident.acknowledge(" ", null, CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actor");
    }

    @Test
    void guestAdvisoryRequiresPublicTitleAndMessage() {
        Incident incident = reported();
        assertThatThrownBy(() -> incident.publishGuestAdvisory(" ", "Stay indoors", "Operator One", null, CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("guestTitle");
        assertThatThrownBy(() -> incident.publishGuestAdvisory("Weather advisory", " ", "Operator One", null, CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("guestMessage");
        assertThat(incident.guestAdvisoryPublished()).isFalse();
        assertThat(incident.version()).isEqualTo(1);
    }

    @Test
    void internalDescriptionDoesNotBecomeAGuestMessage() {
        Incident incident = reported();
        incident.publishGuestAdvisory(
                "Weather advisory",
                "Some outdoor attractions are temporarily paused.",
                "Operator One",
                null,
                CLOCK
        );

        assertThat(incident.hasActiveGuestAdvisory()).isTrue();
        assertThat(incident.guestTitle()).isEqualTo("Weather advisory");
        assertThat(incident.guestMessage()).isEqualTo("Some outdoor attractions are temporarily paused.");
        assertThat(incident.guestMessage()).doesNotContain("Repeated strikes");
        assertThat(incident.internalDescription()).contains("Repeated strikes");
    }

    @Test
    void duplicateAttractionLinksAreRejectedWithoutChangingState() {
        Incident incident = reported();
        assertThatThrownBy(() -> incident.linkAttraction(MANGROVE_RUN, "Operator One", null, CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already linked");
        assertThat(incident.version()).isEqualTo(1);
        assertThat(incident.attractionIds()).containsExactly(MANGROVE_RUN, CYPRESS_COIL);
    }

    @Test
    void linkAndUnlinkAttractionsAreAudited() {
        Incident incident = reported();
        AttractionId stormglass = new AttractionId("stormglass-station");

        incident.linkAttraction(stormglass, "Operator One", null, CLOCK);
        assertThat(incident.attractionIds()).containsExactly(MANGROVE_RUN, CYPRESS_COIL, stormglass);
        assertThat(incident.version()).isEqualTo(2);

        incident.unlinkAttraction(CYPRESS_COIL, "Operator One", "Hold radius no longer includes Cypress Coil", CLOCK);
        assertThat(incident.attractionIds()).containsExactly(MANGROVE_RUN, stormglass);
        assertThat(incident.version()).isEqualTo(3);
        assertThat(incident.activity().getLast()).isInstanceOf(IncidentAttractionUnlinked.class);
    }

    @Test
    void unlinkingRequiresAReasonAndAnExistingLink() {
        Incident incident = reported();
        assertThatThrownBy(() -> incident.unlinkAttraction(MANGROVE_RUN, "Operator One", null, CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNLINK_ATTRACTION");
        assertThatThrownBy(() -> incident.unlinkAttraction(
                new AttractionId("stormglass-station"),
                "Operator One",
                "Not in radius",
                CLOCK
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not linked");
    }

    @Test
    void withdrawRemovesTheActiveGuestProjection() {
        Incident incident = reported();
        incident.publishGuestAdvisory("Weather advisory", "Paused.", "Operator One", null, CLOCK);
        incident.withdrawGuestAdvisory("Operator One", "Advisory no longer needed", CLOCK);

        assertThat(incident.guestAdvisoryPublished()).isFalse();
        assertThat(incident.hasActiveGuestAdvisory()).isFalse();
        assertThat(incident.guestTitle()).isEqualTo("Weather advisory");
        assertThat(incident.version()).isEqualTo(3);
    }

    @Test
    void resolvingUnpublishesTheGuestAdvisory() {
        Incident incident = mitigating();
        incident.publishGuestAdvisory("Weather advisory", "Paused.", "Operator One", null, CLOCK);
        incident.resolve("Operator One", "Storm cell moved out of radius", CLOCK);

        assertThat(incident.status()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(incident.guestAdvisoryPublished()).isFalse();
        assertThat(incident.hasActiveGuestAdvisory()).isFalse();
        assertThat(incident.version()).isEqualTo(5);
    }

    @Test
    void assignAndSeverityChangesAreRecorded() {
        Incident incident = reported();
        incident.assign("Control Tower", "Operator One", null, CLOCK);
        incident.changeSeverity(IncidentSeverity.CRITICAL, "Operator One", "Strikes intensifying", CLOCK);

        assertThat(incident.assignedTo()).isEqualTo("Control Tower");
        assertThat(incident.severity()).isEqualTo(IncidentSeverity.CRITICAL);
        assertThat(incident.version()).isEqualTo(3);
        assertThat(incident.activity().get(1)).isInstanceOf(IncidentAssigned.class);
        assertThat(incident.activity().get(2)).isInstanceOf(IncidentSeverityChanged.class);
    }

    @Test
    void changeSeverityRequiresAReason() {
        Incident incident = reported();
        assertThatThrownBy(() -> incident.changeSeverity(IncidentSeverity.CRITICAL, "Operator One", null, CLOCK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CHANGE_SEVERITY");
    }

    @Test
    void activitiesAreAppendOnly() {
        Incident incident = reported();
        List<IncidentActivity> original = incident.activity();
        incident.acknowledge("Operator One", null, CLOCK);

        assertThatThrownBy(() -> original.add(incident.activity().getLast()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(incident.activity()).hasSize(2);
        assertThat(incident.activity().getFirst().type()).isEqualTo(IncidentEventType.INCIDENT_REPORTED);
    }

    private static Incident reported() {
        return Incident.report(
                "Lightning activity near western basin",
                IncidentType.WEATHER,
                IncidentSeverity.MAJOR,
                "Repeated strikes detected within the hold radius.",
                List.of(MANGROVE_RUN, CYPRESS_COIL),
                "Operator One",
                CLOCK
        );
    }

    private static Incident mitigating() {
        Incident incident = reported();
        incident.acknowledge("Operator One", null, CLOCK);
        incident.startMitigation("Operator One", null, CLOCK);
        return incident;
    }

    private static Incident resolved() {
        Incident incident = mitigating();
        incident.resolve("Operator One", "Storm cell moved out of radius", CLOCK);
        return incident;
    }
}
