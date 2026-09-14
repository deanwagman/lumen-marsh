package com.deanwagman.lumenmarsh.venueops.weather;

import com.deanwagman.lumenmarsh.venueops.security.LocalJwtTokenFactory;
import com.deanwagman.lumenmarsh.venueops.security.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "venueops.attractions.seed=true"
)
@AutoConfigureMockMvc
@DirtiesContext
class WeatherRecommendationSseIntegrationTest {

    private static final String RECOMMENDATION_ID = "bbbbbbbb-cccc-dddd-eeee-ffffffffffff";

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalJwtTokenFactory tokens;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    void operatorStreamReceivesWeatherEventsAndGuestStreamsDoNot() throws Exception {
        HttpResponse<java.io.InputStream> operatorResponse = openStream("/api/v1/operator/events");
        HttpResponse<java.io.InputStream> guestResponse = openStream("/api/v1/attractions/events");
        HttpResponse<java.io.InputStream> parkResponse = openStream("/api/v1/events");

        BlockingQueue<SseMessage> operatorMessages = new LinkedBlockingQueue<>();
        BlockingQueue<SseMessage> guestMessages = new LinkedBlockingQueue<>();
        BlockingQueue<SseMessage> parkMessages = new LinkedBlockingQueue<>();
        Thread operatorReader = Thread.ofVirtual().start(() -> readSse(operatorResponse.body(), operatorMessages));
        Thread guestReader = Thread.ofVirtual().start(() -> readSse(guestResponse.body(), guestMessages));
        Thread parkReader = Thread.ofVirtual().start(() -> readSse(parkResponse.body(), parkMessages));
        try {
            take(operatorMessages, "attractions.snapshot");
            SseMessage weatherSnapshot = take(operatorMessages, "weather.recommendations.snapshot");
            assertThat(weatherSnapshot.data()).isEqualTo("[]");

            take(guestMessages, "attractions.snapshot");
            take(parkMessages, "attractions.snapshot");
            take(parkMessages, "advisories.snapshot");

            mockMvc.perform(post("/api/v1/integrations/weather/recommendations")
                        .with(TestAuth.weatherService())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(activeBody()))
                    .andExpect(status().isCreated());

            SseMessage updated = take(operatorMessages, "weather.recommendation.updated");
            JsonNode payload = jsonMapper.readTree(updated.data());
            assertThat(updated.id()).isEqualTo(payload.get("eventId").asString());
            assertThat(payload.get("recommendation").get("id").asString()).isEqualTo(RECOMMENDATION_ID);
            assertThat(payload.get("recommendation").get("status").asString()).isEqualTo("ACTIVE");
            assertThat(payload.get("recommendation").get("evidence").asString()).contains("Simulated lightning");

            mockMvc.perform(post("/api/v1/integrations/weather/recommendations")
                        .with(TestAuth.weatherService())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(clearedBody()))
                    .andExpect(status().isOk());

            SseMessage cleared = take(operatorMessages, "weather.recommendation.cleared");
            JsonNode clearedPayload = jsonMapper.readTree(cleared.data());
            assertThat(clearedPayload.get("recommendation").get("status").asString()).isEqualTo("CLEARED");

            assertThat(pollNamed(guestMessages, "weather.recommendation.updated")).isNull();
            assertThat(pollNamed(guestMessages, "weather.recommendation.cleared")).isNull();
            assertThat(pollNamed(parkMessages, "weather.recommendation.updated")).isNull();
            assertThat(pollNamed(parkMessages, "weather.recommendation.cleared")).isNull();
        } finally {
            operatorReader.interrupt();
            guestReader.interrupt();
            parkReader.interrupt();
            operatorResponse.body().close();
            guestResponse.body().close();
            parkResponse.body().close();
        }
    }

    private HttpResponse<java.io.InputStream> openStream(String path) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", MediaType.TEXT_EVENT_STREAM_VALUE)
                .GET();
        if (path.startsWith("/api/v1/operator/")) {
            builder.header("Authorization", "Bearer " + tokens.operatorToken());
        }
        HttpResponse<java.io.InputStream> response = httpClient.sendAsync(
                builder.build(),
                HttpResponse.BodyHandlers.ofInputStream()
        ).get(10, TimeUnit.SECONDS);
        assertThat(response.statusCode()).isEqualTo(200);
        return response;
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

    private static SseMessage pollNamed(BlockingQueue<SseMessage> messages, String eventName) throws InterruptedException {
        SseMessage message = messages.poll(400, TimeUnit.MILLISECONDS);
        while (message != null) {
            if (eventName.equals(message.event())) {
                return message;
            }
            message = messages.poll(400, TimeUnit.MILLISECONDS);
        }
        return null;
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

    private static String activeBody() {
        return """
                {
                  "recommendationId": "%s",
                  "ruleId": "simulated-lightning-hold",
                  "status": "ACTIVE",
                  "severity": "WARNING",
                  "summary": "Place Mangrove Run and Cypress Coil on weather hold",
                  "evidence": "Simulated lightning strike 1.2 miles from the western basin.",
                  "recommendedAction": "Place Mangrove Run and Cypress Coil on weather hold",
                  "affectedAttractionIds": ["mangrove-run", "cypress-coil"],
                  "observedAt": "2026-09-01T16:00:00Z",
                  "version": 1
                }
                """.formatted(RECOMMENDATION_ID);
    }

    private static String clearedBody() {
        return """
                {
                  "recommendationId": "%s",
                  "ruleId": "simulated-lightning-hold",
                  "status": "CLEARED",
                  "severity": "INFO",
                  "summary": "Lightning hold can be reviewed for clearance",
                  "evidence": "Simulated lightning is outside the hold radius.",
                  "recommendedAction": "Review weather holds for return-to-service",
                  "affectedAttractionIds": ["mangrove-run", "cypress-coil"],
                  "observedAt": "2026-09-01T16:30:00Z",
                  "version": 2
                }
                """.formatted(RECOMMENDATION_ID);
    }

    private record SseMessage(String id, String event, String data) {
    }
}
