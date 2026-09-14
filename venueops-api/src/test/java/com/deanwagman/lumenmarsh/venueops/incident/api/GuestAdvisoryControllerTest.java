package com.deanwagman.lumenmarsh.venueops.incident.api;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentService;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;
import com.deanwagman.lumenmarsh.venueops.security.ImportVenueOpsSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ImportVenueOpsSecurity
@WebMvcTest(controllers = {GuestAdvisoryController.class, IncidentExceptionHandler.class})
class GuestAdvisoryControllerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T15:30:00Z"), ZoneOffset.UTC);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentService incidentService;

    @Test
    void listsOnlyGuestSafeAdvisoryFields() throws Exception {
        Incident incident = Incident.report(
                new IncidentId("inc-1"),
                "Lightning activity near western basin",
                IncidentType.WEATHER,
                IncidentSeverity.MAJOR,
                "Repeated strikes detected within the hold radius.",
                List.of(new AttractionId("mangrove-run"), new AttractionId("cypress-coil")),
                "Operator One",
                CLOCK
        );
        incident.publishGuestAdvisory(
                "Weather advisory",
                "Some outdoor attractions are temporarily paused.",
                "Operator One",
                null,
                CLOCK
        );
        when(incidentService.listActiveGuestAdvisories()).thenReturn(List.of(incident));

        mockMvc.perform(get("/api/v1/advisories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("inc-1"))
                .andExpect(jsonPath("$[0].severity").value("MAJOR"))
                .andExpect(jsonPath("$[0].title").value("Weather advisory"))
                .andExpect(jsonPath("$[0].message").value("Some outdoor attractions are temporarily paused."))
                .andExpect(jsonPath("$[0].affectedAttractionIds", contains("mangrove-run", "cypress-coil")))
                .andExpect(jsonPath("$[0].internalDescription").doesNotExist())
                .andExpect(jsonPath("$[0].assignedTo").doesNotExist())
                .andExpect(jsonPath("$[0].actor").doesNotExist())
                .andExpect(jsonPath("$[0].reason").doesNotExist())
                .andExpect(jsonPath("$[0].activity").doesNotExist())
                .andExpect(jsonPath("$.[*].*", not(contains("Operator One"))))
                .andExpect(jsonPath("$[0].message").value(not("Repeated strikes detected within the hold radius.")));
    }
}
