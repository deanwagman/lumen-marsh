package com.deanwagman.lumenmarsh.venueops.demo;

import com.deanwagman.lumenmarsh.venueops.historical.HistoricalModels;
import com.deanwagman.lumenmarsh.venueops.maintenance.application.MaintenanceRecommendationRepository;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.recommendation.MaintenanceRecommendationId;
import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "venueops.attractions.persistence=jpa",
        "venueops.attractions.seed=true",
        "venueops.demo-sandbox.enabled=true",
        "venueops.demo-sandbox.internal-secret=0123456789abcdef0123456789abcdef",
        "venueops.demo-sandbox.secure-cookie=false",
        "venueops.security.issuer-uri=https://demo.localhost:8443",
        "spring.autoconfigure.exclude="
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class DemoSandboxPostgresIntegrationTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("venueops")
            .withUsername("venueops")
            .withPassword("venueops");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoLeaseStore store;

    @Autowired
    private DemoSandboxProperties properties;

    @Autowired
    private MaintenanceRecommendationRepository maintenanceRecommendations;

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void onlyOneConcurrentReservationWinsAndForgedTokensFail() throws Exception {
        DemoLeaseStore.Lease existing = store.find();
        if (existing.tokenHash() != null) {
            store.expire(existing.tokenHash());
        }
        Instant now = Instant.now();
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Callable<Boolean>> attempts = java.util.stream.IntStream.range(0, 16)
                    .mapToObj(index -> (Callable<Boolean>) () ->
                            store.reserve(DemoTokens.hash("candidate-" + index), now, properties).isPresent())
                    .toList();
            long winners = executor.invokeAll(attempts).stream()
                    .filter(future -> {
                        try {
                            return future.get();
                        } catch (Exception failure) {
                            throw new AssertionError(failure);
                        }
                    })
                    .count();
            org.junit.jupiter.api.Assertions.assertEquals(1, winners);
        }
        org.junit.jupiter.api.Assertions.assertTrue(
                store.authenticate(DemoTokens.hash("forged"), now, properties, false).isEmpty());
        store.expire(store.find().tokenHash());
    }

    @Test
    void expiredCredentialCannotMutateAndAReplacementCanReserve() {
        DemoLeaseStore.Lease existing = store.find();
        if (existing.tokenHash() != null) {
            store.expire(existing.tokenHash());
        }
        DemoSandboxProperties shortLease = new DemoSandboxProperties(
                true, SECRET, "", "", "", "", "",
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(2), 30, false);
        Instant issued = Instant.now();
        String staleHash = DemoTokens.hash("stale");
        org.junit.jupiter.api.Assertions.assertTrue(store.reserve(staleHash, issued, shortLease).isPresent());
        store.activate(staleHash, issued, shortLease);
        Instant expiredAt = issued.plusSeconds(3);
        org.junit.jupiter.api.Assertions.assertTrue(
                store.authenticate(staleHash, expiredAt, shortLease, false).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(
                store.reserve(DemoTokens.hash("replacement"), expiredAt, shortLease).isPresent());
        store.expire(store.find().tokenHash());
    }

    @Test
    void failedResetKeepsExclusiveLeaseAndReusesResetIdentity() {
        DemoLeaseStore.Lease existing = store.find();
        if (existing.tokenHash() != null) {
            store.expire(existing.tokenHash());
        }
        Instant now = Instant.now();
        String hash = DemoTokens.hash("retry-holder");
        UUID firstReset = UUID.randomUUID();
        org.junit.jupiter.api.Assertions.assertTrue(store.reserve(hash, now, properties).isPresent());
        store.activate(hash, now, properties);

        DemoLeaseStore.Lease first = store.beginReset(
                hash, firstReset.toString(), now, properties
        ).orElseThrow();
        store.fail(hash, "reliability unavailable");

        org.junit.jupiter.api.Assertions.assertTrue(
                store.reserve(DemoTokens.hash("spectator"), now.plusSeconds(1), properties).isEmpty());
        DemoLeaseStore.Lease retry = store.beginReset(
                hash, UUID.randomUUID().toString(), now.plusSeconds(1), properties
        ).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(first.resetId(), retry.resetId());
        org.junit.jupiter.api.Assertions.assertEquals(firstReset.toString(), retry.resetId());
        store.expire(hash);
    }

    @Test
    void operatorMutationNeedsLeaseEvenWithValidOperatorIdentity() throws Exception {
        mockMvc.perform(post("/api/v1/operator/attractions/mangrove-run/commands")
                        .with(TestAuth.supervisor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commandId":"8ad6ff18-dbbc-4c55-a190-a8f4fdd24cde",
                                 "command":{"type":"UPDATE_WAIT_TIME","waitMinutes":12,"expectedVersion":0}}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Active sandbox lease required"));
    }

    @Test
    void spectatorCanReadTheOperatingStoryWhileReplayStaysLeaseGated() throws Exception {
        mockMvc.perform(get("/api/v1/operator/analytics/availability")
                        .header(DemoGatewayAuthenticationFilter.SECRET_HEADER, SECRET)
                        .param("datasetId", HistoricalModels.FIXTURE_DATASET_ID)
                        .param("from", "2026-10-13")
                        .param("to", "2026-10-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answers.length()").value(3))
                .andExpect(jsonPath("$.answers[0].question").value("Which attraction had the worst uptime?"))
                .andExpect(jsonPath("$.answers[1].answer").value(
                        org.hamcrest.Matchers.containsString("maintenance episode")))
                .andExpect(jsonPath("$.answers[2].answer").value(
                        org.hamcrest.Matchers.containsString("2026-10-15")));

        mockMvc.perform(post("/api/v1/operator/analytics/datasets/"
                        + HistoricalModels.FIXTURE_DATASET_ID + "/replay")
                        .header(DemoGatewayAuthenticationFilter.SECRET_HEADER, SECRET))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Active sandbox lease required"));
    }

    @Test
    void privateGatewayCanReadForSpectatorsButForgedGatewayIdentityCannot() throws Exception {
        mockMvc.perform(get("/api/v1/operator/dashboard")
                        .header(DemoGatewayAuthenticationFilter.SECRET_HEADER, SECRET))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/operator/dashboard")
                        .header(DemoGatewayAuthenticationFilter.SECRET_HEADER, "forged"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void privateGatewayIssuerSupportsAuditedMaintenanceDismissal() throws Exception {
        DemoLeaseStore.Lease existing = store.find();
        if (existing.tokenHash() != null) {
            store.expire(existing.tokenHash());
        }
        String leaseToken = "audited-maintenance-holder";
        Instant now = Instant.now();
        String leaseHash = DemoTokens.hash(leaseToken);
        store.reserve(leaseHash, now, properties).orElseThrow();
        store.activate(leaseHash, now, properties);

        MvcResult ingest = mockMvc.perform(post("/api/v1/integrations/reliability/recommendations")
                        .with(TestAuth.reliabilityService())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "observationId": "demo-gateway-audit-issuer",
                                  "observedAt": "2026-09-14T18:25:00Z",
                                  "assetCode": "CC-TRAIN-01-WHEEL-A",
                                  "signalType": "VIBRATION",
                                  "severity": "WARNING",
                                  "value": 8.4,
                                  "unit": "mm/s",
                                  "evidence": "Sandbox issuer audit test.",
                                  "recommendedAction": "Inspect the wheel assembly."
                                }
                                """))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        JsonNode body = json.readTree(ingest.getResponse().getContentAsByteArray());
        MaintenanceRecommendationId recommendationId =
                new MaintenanceRecommendationId(UUID.fromString(body.get("recommendationId").asString()));

        mockMvc.perform(post("/api/v1/operator/maintenance/recommendations/"
                        + recommendationId.value() + "/commands")
                        .header(DemoGatewayAuthenticationFilter.SECRET_HEADER, SECRET)
                        .cookie(new Cookie(DemoLeaseService.COOKIE, leaseToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "commandId": "%s",
                                  "type": "DISMISS",
                                  "expectedVersion": 1,
                                  "reason": "Audited sandbox dismissal"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));

        assertThat(maintenanceRecommendations.findById(recommendationId).orElseThrow().reviewedBy())
                .isEqualTo("Demo Visitor");
        store.expire(leaseHash);
    }

    @Test
    void internalResetRejectsSecretsAndReplaysCompletedResetId() throws Exception {
        UUID resetId = UUID.randomUUID();
        String body = "{\"resetId\":\"" + resetId + "\"}";
        mockMvc.perform(post("/internal/demo/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/demo/reset")
                        .header("X-Demo-Reset-Secret", "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/demo/reset")
                        .header("X-Demo-Reset-Secret", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(post("/internal/demo/reset")
                        .header("X-Demo-Reset-Secret", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REPLAY"));
    }
}
