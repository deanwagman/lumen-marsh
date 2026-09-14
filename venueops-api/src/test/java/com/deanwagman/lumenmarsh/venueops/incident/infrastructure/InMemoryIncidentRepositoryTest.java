package com.deanwagman.lumenmarsh.venueops.incident.infrastructure;

import com.deanwagman.lumenmarsh.venueops.incident.application.StaleIncidentVersionException;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryIncidentRepositoryTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T15:30:00Z"), ZoneOffset.UTC);

    @Test
    void concurrentSavesRejectStaleVersions() {
        InMemoryIncidentRepository repository = new InMemoryIncidentRepository();
        Incident original = Incident.report(
                "Lightning activity near western basin",
                IncidentType.WEATHER,
                IncidentSeverity.MAJOR,
                null,
                List.of(),
                "Operator One",
                CLOCK
        );
        repository.save(original);

        Incident first = repository.findById(original.id()).orElseThrow();
        Incident second = repository.findById(original.id()).orElseThrow();
        first.acknowledge("Operator One", null, CLOCK);
        second.acknowledge("Operator One", null, CLOCK);
        repository.save(first);

        assertThatThrownBy(() -> repository.save(second))
                .isInstanceOf(StaleIncidentVersionException.class);
        assertThat(repository.findById(original.id()).orElseThrow().version()).isEqualTo(2);
    }
}
