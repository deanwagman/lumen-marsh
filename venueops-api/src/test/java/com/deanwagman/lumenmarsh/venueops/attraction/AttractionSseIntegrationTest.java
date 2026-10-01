package com.deanwagman.lumenmarsh.venueops.attraction;

import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import com.deanwagman.lumenmarsh.venueops.testsupport.CommandJson;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class AttractionSseIntegrationTest {

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
    void streamReceivesSnapshotThenOperationalUpdates() throws Exception {
        HttpRequest sseRequest = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/attractions/events"))
                .header("Accept", MediaType.TEXT_EVENT_STREAM_VALUE)
                .GET()
                .build();
        HttpResponse<java.io.InputStream> response = httpClient.sendAsync(
                sseRequest,
                HttpResponse.BodyHandlers.ofInputStream()
        ).get(10, TimeUnit.SECONDS);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .contains("text/event-stream");
        assertThat(response.headers().firstValue("Cache-Control").orElse(""))
                .contains("no-cache");

        BlockingQueue<SseMessage> messages = new LinkedBlockingQueue<>();
        Thread reader = Thread.ofVirtual().start(() -> readSse(response.body(), messages));
        try {
            SseMessage snapshot = take(messages, "attractions.snapshot");
            assertThat(snapshot.data().toString()).contains("mangrove-run");
            assertThat(snapshot.data().toString()).doesNotContain("\"actor\"");

            mockMvc.perform(post("/api/v1/operator/attractions/mangrove-run/commands")
                            .with(TestAuth.operator())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CommandJson.envelope("""
                                    {"type":"UPDATE_WAIT_TIME","waitMinutes":35,"expectedVersion":0}
                                    """)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.waitMinutes").value(35))
                    .andExpect(jsonPath("$.version").value(1));

            SseMessage waitUpdate = take(messages, "attraction.updated");
            JsonNode waitPayload = jsonMapper.readTree(waitUpdate.data());
            assertThat(waitUpdate.id()).isNotBlank();
            assertThat(waitPayload.get("eventId").asString()).isEqualTo(waitUpdate.id());
            assertThat(waitPayload.get("eventType").asString()).isEqualTo("WAIT_TIME_CHANGED");
            assertThat(waitPayload.get("attraction").get("id").asString()).isEqualTo("mangrove-run");
            assertThat(waitPayload.get("attraction").get("waitMinutes").asInt()).isEqualTo(35);
            assertThat(waitPayload.get("attraction").get("version").asLong()).isEqualTo(1);
            assertThat(waitPayload.toString()).doesNotContain("Operator One");

            mockMvc.perform(post("/api/v1/operator/attractions/mangrove-run/commands")
                            .with(TestAuth.operator())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CommandJson.envelope("""
                                    {"type":"PLACE_WEATHER_HOLD","reason":"Lightning detected within operating radius","expectedVersion":1}
                                    """)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("WEATHER_HOLD"));

            SseMessage statusUpdate = take(messages, "attraction.updated");
            JsonNode statusPayload = jsonMapper.readTree(statusUpdate.data());
            assertThat(statusPayload.get("eventType").asString()).isEqualTo("STATUS_CHANGED");
            assertThat(statusPayload.get("attraction").get("status").asString()).isEqualTo("WEATHER_HOLD");
            assertThat(statusPayload.get("attraction").get("capacityMode").asString()).isEqualTo("NOT_APPLICABLE");
            assertThat(statusPayload.get("attraction").get("waitMinutes").isNull()).isTrue();
            assertThat(statusPayload.get("attraction").get("version").asLong()).isEqualTo(2);
            assertThat(statusPayload.get("attraction").get("statusMessage").asString())
                    .contains("weather");
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
