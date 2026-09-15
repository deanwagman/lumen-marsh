package com.deanwagman.lumenmarsh.venueops.incident.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentActivity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentEventType;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentActivityMapperTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T15:30:00Z"), ZoneOffset.UTC);

    @ParameterizedTest
    @EnumSource(IncidentEventType.class)
    void roundTripsEveryEventType(IncidentEventType type) {
        Incident incident = fullyExercisedIncident();
        IncidentActivity original = incident.activity().stream()
                .filter(activity -> activity.type() == type)
                .findFirst()
                .orElseThrow();

        IncidentActivity restored = IncidentActivityMapper.toDomain(IncidentActivityMapper.toEntity(original));
        assertThat(restored).isEqualTo(original);
    }

    private static Incident fullyExercisedIncident() {
        AttractionId mangrove = new AttractionId("mangrove-run");
        AttractionId cypress = new AttractionId("cypress-coil");
        AttractionId stormglass = new AttractionId("stormglass-station");
        Incident incident = Incident.report(
                "Lightning activity near western basin",
                IncidentType.WEATHER,
                IncidentSeverity.MAJOR,
                "Internal",
                List.of(mangrove),
                "Operator One",
                CLOCK
        );
        incident.linkAttraction(cypress, "Operator One", null, CLOCK);
        incident.acknowledge("Operator One", null, CLOCK);
        incident.assign("Control Tower", "Operator One", null, CLOCK);
        incident.changeSeverity(IncidentSeverity.CRITICAL, "Operator One", "Strikes intensifying", CLOCK);
        incident.publishGuestAdvisory("Weather advisory", "Paused.", "Operator One", null, CLOCK);
        incident.withdrawGuestAdvisory("Operator One", "Rewording", CLOCK);
        incident.publishGuestAdvisory("Weather advisory", "Some outdoor attractions are paused.", "Operator One", null, CLOCK);
        incident.startMitigation("Operator One", null, CLOCK);
        incident.recordLinkedWorkOrder("6dbb04f2-b20d-4b16-ae57-43072fc2e408", "LM-2026-0042", "Operator One", "Maintenance opened", CLOCK);
        incident.unlinkAttraction(cypress, "Operator One", "Not in radius", CLOCK);
        incident.linkAttraction(stormglass, "Operator One", null, CLOCK);
        incident.resolve("Operator One", "Storm cell moved out of radius", CLOCK);
        return incident;
    }
}
