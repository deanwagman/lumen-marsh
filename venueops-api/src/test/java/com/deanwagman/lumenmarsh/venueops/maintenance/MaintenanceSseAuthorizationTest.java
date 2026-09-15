package com.deanwagman.lumenmarsh.venueops.maintenance;

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
class MaintenanceSseAuthorizationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalJwtTokenFactory tokens;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    void maintenanceEventsRequireMaintenanceReadOnTheOperatorStream() throws Exception {
        HttpResponse<java.io.InputStream> readerResponse = openStream(tokens.operatorWithoutMaintenanceToken());
        HttpResponse<java.io.InputStream> maintainerResponse = openStream(tokens.operatorToken());

        BlockingQueue<SseMessage> readerMessages = new LinkedBlockingQueue<>();
        BlockingQueue<SseMessage> maintainerMessages = new LinkedBlockingQueue<>();
        Thread reader = Thread.ofVirtual().start(() -> readSse(readerResponse.body(), readerMessages));
        Thread maintainer = Thread.ofVirtual().start(() -> readSse(maintainerResponse.body(), maintainerMessages));
        try {
            take(readerMessages, "attractions.snapshot");
            take(maintainerMessages, "attractions.snapshot");
            take(maintainerMessages, "maintenance.work-orders.snapshot");
            assertThat(pollNamed(readerMessages, "maintenance.work-orders.snapshot")).isNull();

            mockMvc.perform(post("/api/v1/operator/maintenance/work-orders")
                            .with(TestAuth.operator())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "commandId": "ce8d795d-cc8c-4f10-a1ac-4eb331e727ab",
                                      "assetId": "0fd7c7ce-f7af-4b65-8789-27679ca40303",
                                      "classification": "CORRECTIVE",
                                      "priority": "P3",
                                      "summary": "SSE authorization coverage"
                                    }
                                    """))
                    .andExpect(status().isCreated());

            take(maintainerMessages, "maintenance.work-order.updated");
            assertThat(pollNamed(readerMessages, "maintenance.work-order.updated")).isNull();
        } finally {
            reader.interrupt();
            maintainer.interrupt();
            readerResponse.body().close();
            maintainerResponse.body().close();
        }
    }

    private HttpResponse<java.io.InputStream> openStream(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/operator/events"))
                .header("Accept", MediaType.TEXT_EVENT_STREAM_VALUE)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<java.io.InputStream> response = httpClient.sendAsync(
                request,
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

    private record SseMessage(String id, String event, String data) {
    }
}
