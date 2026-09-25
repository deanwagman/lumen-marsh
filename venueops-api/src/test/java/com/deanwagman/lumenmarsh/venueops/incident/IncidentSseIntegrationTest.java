package com.deanwagman.lumenmarsh.venueops.incident;

import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import com.deanwagman.lumenmarsh.venueops.testsupport.CommandJson;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "venueops.attractions.seed=true"
)
@AutoConfigureMockMvc
@DirtiesContext
class IncidentSseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    void parkStreamReceivesGuestAdvisorySnapshotsAndUpdates() throws Exception {
        HttpRequest sseRequest = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/events"))
                .header("Accept", MediaType.TEXT_EVENT_STREAM_VALUE)
                .GET()
                .build();
        HttpResponse<java.io.InputStream> response = httpClient.sendAsync(
                sseRequest,
                HttpResponse.BodyHandlers.ofInputStream()
        ).get(10, TimeUnit.SECONDS);
        assertThat(response.statusCode()).isEqualTo(200);

        BlockingQueue<SseMessage> messages = new LinkedBlockingQueue<>();
        Thread reader = Thread.ofVirtual().start(() -> readSse(response.body(), messages));
        try {
            SseMessage attractionsSnapshot = take(messages, "attractions.snapshot");
            assertThat(attractionsSnapshot.data()).contains("mangrove-run");
            take(messages, "advisories.snapshot");

            MvcResult created = mockMvc.perform(post("/api/v1/operator/incidents")
                            .with(TestAuth.operator())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Lightning activity near western basin",
                                      "type": "WEATHER",
                                      "severity": "MAJOR",
                                      "internalDescription": "Repeated strikes detected within the hold radius.",
                                      "attractionIds": ["mangrove-run"]
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.version").value(1))
                    .andReturn();
            JsonNode createdBody = jsonMapper.readTree(created.getResponse().getContentAsByteArray());
            String incidentId = createdBody.get("id").asString();

            mockMvc.perform(post("/api/v1/operator/incidents/" + incidentId + "/commands")
                            .with(TestAuth.operator())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CommandJson.envelope("""
                                    {"type":"ACKNOWLEDGE","expectedVersion":1}
                                    """)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/operator/incidents/" + incidentId + "/commands")
                            .with(TestAuth.supervisor())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CommandJson.envelope("""
                                    {
                                      "type":"PUBLISH_GUEST_ADVISORY",
                                      "guestTitle":"Weather advisory",
                                      "guestMessage":"Some outdoor attractions are temporarily paused.",
                                      "expectedVersion":2
                                    }
                                    """)))
                    .andExpect(status().isOk());

            SseMessage published = take(messages, "advisory.published");
            JsonNode publishedPayload = jsonMapper.readTree(published.data());
            assertThat(published.id()).isEqualTo(publishedPayload.get("eventId").asString());
            assertThat(publishedPayload.get("advisory").get("id").asString()).isEqualTo(incidentId);
            assertThat(publishedPayload.get("advisory").get("title").asString()).isEqualTo("Weather advisory");
            assertThat(publishedPayload.toString()).doesNotContain("Repeated strikes");
            assertThat(publishedPayload.toString()).doesNotContain("Operator One");
            assertThat(publishedPayload.toString()).doesNotContain("assignedTo");
            assertThat(publishedPayload.toString()).doesNotContain("internalDescription");
            assertThat(publishedPayload.get("advisory").has("internalDescription")).isFalse();
            assertThat(publishedPayload.get("advisory").has("assignedTo")).isFalse();
            assertThat(publishedPayload.get("advisory").has("actor")).isFalse();

            mockMvc.perform(post("/api/v1/operator/incidents/" + incidentId + "/commands")
                            .with(TestAuth.supervisor())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CommandJson.envelope("""
                                    {
                                      "type":"WITHDRAW_GUEST_ADVISORY",
                                      "reason":"Hold cleared for guest demo",
                                      "expectedVersion":3
                                    }
                                    """)))
                    .andExpect(status().isOk());

            SseMessage withdrawn = take(messages, "advisory.withdrawn");
            JsonNode withdrawnPayload = jsonMapper.readTree(withdrawn.data());
            assertThat(withdrawnPayload.get("advisory").get("id").asString()).isEqualTo(incidentId);
            assertThat(withdrawnPayload.toString()).doesNotContain("Repeated strikes");
            assertThat(withdrawnPayload.toString()).doesNotContain("assignedTo");
            assertThat(withdrawnPayload.toString()).doesNotContain("internalDescription");
        } finally {
            reader.interrupt();
            response.body().close();
        }
    }

    private static SseMessage take(BlockingQueue<SseMessage> messages, String eventName) throws InterruptedException {
        SseMessage message = messages.poll(10, TimeUnit.SECONDS);
        while (message != null && !"heartbeat".equals(message.event()) && !eventName.equals(message.event())) {
            message = messages.poll(10, TimeUnit.SECONDS);
        }
        assertThat(message).as("expected SSE event %s", eventName).isNotNull();
        assertThat(message.event()).isEqualTo(eventName);
        return message;
    }

    private static void readSse(java.io.InputStream body, BlockingQueue<SseMessage> messages) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String event = null;
            String id = null;
            List<String> data = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (event != null || !data.isEmpty()) {
                        messages.add(new SseMessage(id, event, String.join("", data)));
                    }
                    event = null;
                    id = null;
                    data = new ArrayList<>();
                    continue;
                }
                if (line.startsWith(":")) {
                    continue;
                }
                if (line.startsWith("event:")) {
                    event = line.substring("event:".length()).trim();
                } else if (line.startsWith("id:")) {
                    id = line.substring("id:".length()).trim();
                } else if (line.startsWith("data:")) {
                    data.add(line.substring("data:".length()).trim());
                }
            }
        } catch (Exception ignored) {
            // Stream closed when the test finishes.
        }
    }

    private record SseMessage(String id, String event, String data) {
    }
}
