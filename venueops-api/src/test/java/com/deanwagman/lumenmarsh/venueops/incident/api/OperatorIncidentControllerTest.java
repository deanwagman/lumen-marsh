package com.deanwagman.lumenmarsh.venueops.incident.api;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionNotFoundException;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionId;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentNotFoundException;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentService;
import com.deanwagman.lumenmarsh.venueops.incident.application.RelatedMaintenanceWorkOrders;
import com.deanwagman.lumenmarsh.venueops.incident.application.StaleIncidentVersionException;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentCommand;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentId;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentStatus;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;
import com.deanwagman.lumenmarsh.venueops.incident.domain.InvalidIncidentTransitionException;
import com.deanwagman.lumenmarsh.venueops.security.ImportVenueOpsSecurity;
import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ImportVenueOpsSecurity
@WebMvcTest(controllers = {OperatorIncidentController.class, IncidentExceptionHandler.class})
class OperatorIncidentControllerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-01T15:30:00Z"), ZoneOffset.UTC);
    private static final IncidentId INCIDENT_ID = new IncidentId("inc-1");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentService incidentService;

    @MockitoBean
    private RelatedMaintenanceWorkOrders relatedMaintenanceWorkOrders;

    @Test
    void reportCreatesIncident() throws Exception {
        when(incidentService.report(
                eq("Lightning activity near western basin"),
                eq(IncidentType.WEATHER),
                eq(IncidentSeverity.MAJOR),
                eq("Repeated strikes detected within the hold radius."),
                eq(List.of("mangrove-run", "cypress-coil")),
                eq("Operator One")
        )).thenReturn(reported());

        mockMvc.perform(post("/api/v1/operator/incidents")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Lightning activity near western basin",
                                  "type": "WEATHER",
                                  "severity": "MAJOR",
                                  "internalDescription": "Repeated strikes detected within the hold radius.",
                                  "attractionIds": ["mangrove-run", "cypress-coil"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("inc-1"))
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.internalDescription").value("Repeated strikes detected within the hold radius."));
    }

    @Test
    void missingIncidentReturns404() throws Exception {
        when(incidentService.get(INCIDENT_ID)).thenThrow(new IncidentNotFoundException(INCIDENT_ID));

        mockMvc.perform(get("/api/v1/operator/incidents/inc-1").with(TestAuth.operator()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INCIDENT_NOT_FOUND"));
    }

    @Test
    void invalidTransitionReturnsConflict() throws Exception {
        when(incidentService.get(INCIDENT_ID)).thenReturn(reported());
        when(incidentService.execute(
                eq(INCIDENT_ID),
                eq(IncidentCommand.RESOLVE),
                eq("Supervisor One"),
                eq("Too soon"),
                eq(1L),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(false)
        )).thenThrow(new InvalidIncidentTransitionException(IncidentStatus.REPORTED, IncidentCommand.RESOLVE));

        mockMvc.perform(post("/api/v1/operator/incidents/inc-1/commands")
                        .with(TestAuth.supervisor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "RESOLVE",
                                  "reason": "Too soon",
                                  "expectedVersion": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void staleVersionReturnsConflict() throws Exception {
        when(incidentService.get(INCIDENT_ID)).thenReturn(reported());
        when(incidentService.execute(
                eq(INCIDENT_ID),
                eq(IncidentCommand.ACKNOWLEDGE),
                eq("Operator One"),
                isNull(),
                eq(9L),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(false)
        )).thenThrow(new StaleIncidentVersionException(INCIDENT_ID, 9L, 1L));

        mockMvc.perform(post("/api/v1/operator/incidents/inc-1/commands")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "ACKNOWLEDGE",
                                  "expectedVersion": 9
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"))
                .andExpect(jsonPath("$.expectedVersion").value(9))
                .andExpect(jsonPath("$.actualVersion").value(1));
    }

    @Test
    void unknownAttractionLinkReturns404() throws Exception {
        when(incidentService.report(
                eq("Lightning activity near western basin"),
                eq(IncidentType.WEATHER),
                eq(IncidentSeverity.MAJOR),
                isNull(),
                eq(List.of("missing-ride")),
                eq("Operator One")
        )).thenThrow(new AttractionNotFoundException(new AttractionId("missing-ride")));

        mockMvc.perform(post("/api/v1/operator/incidents")
                        .with(TestAuth.operator())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Lightning activity near western basin",
                                  "type": "WEATHER",
                                  "severity": "MAJOR",
                                  "attractionIds": ["missing-ride"]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ATTRACTION_NOT_FOUND"));
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/operator/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Lightning activity near western basin",
                                  "type": "WEATHER",
                                  "severity": "MAJOR"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private static Incident reported() {
        return Incident.report(
                INCIDENT_ID,
                "Lightning activity near western basin",
                IncidentType.WEATHER,
                IncidentSeverity.MAJOR,
                "Repeated strikes detected within the hold radius.",
                List.of(new AttractionId("mangrove-run"), new AttractionId("cypress-coil")),
                "Operator One",
                CLOCK
        );
    }
}
