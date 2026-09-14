package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.streaming;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionOperationalSnapshot;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionUpdateEventType;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import com.deanwagman.lumenmarsh.venueops.incident.application.GuestAdvisoryOperationalSnapshot;
import com.deanwagman.lumenmarsh.venueops.incident.application.GuestAdvisoryOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.incident.application.GuestAdvisoryUpdateEventType;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentOperationalSnapshot;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentUpdateEventType;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentStatus;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentType;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationOperationalSnapshot;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationUpdateEventType;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationOperatorStatus;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SseAttractionUpdateBroadcasterTest {

    private static final Instant NOW = Instant.parse("2026-08-27T19:20:00Z");

    private SseAttractionUpdateBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        broadcaster = new SseAttractionUpdateBroadcaster(new SimpleMeterRegistry(), 1_800_000L, 3_000L);
    }

    @Test
    void updatesReachEveryActiveSubscriber() {
        RecordingEmitter first = new RecordingEmitter();
        RecordingEmitter second = new RecordingEmitter();
        broadcaster.subscribe(first);
        broadcaster.subscribe(second);

        AttractionOperationalUpdate update = sampleUpdate("evt-1", 35, 7L);
        broadcaster.publish(update);

        assertThat(first.payloads).containsExactly(update);
        assertThat(second.payloads).containsExactly(update);
        assertThat(broadcaster.subscriberCount()).isEqualTo(2);
    }

    @Test
    void failedSubscriberIsRemovedWithoutAffectingOthers() {
        RecordingEmitter healthy = new RecordingEmitter();
        RecordingEmitter failing = new RecordingEmitter(new IOException("disconnected"));
        broadcaster.subscribe(healthy);
        broadcaster.subscribe(failing);

        AttractionOperationalUpdate update = sampleUpdate("evt-2", 40, 8L);
        assertThatCode(() -> broadcaster.publish(update)).doesNotThrowAnyException();

        assertThat(healthy.payloads).containsExactly(update);
        assertThat(broadcaster.subscriberCount()).isEqualTo(1);
    }

    @Test
    void completedSubscribersAreRemoved() {
        CallbackEmitter emitter = new CallbackEmitter();
        broadcaster.subscribe(emitter);
        assertThat(broadcaster.subscriberCount()).isEqualTo(1);

        emitter.completion.run();
        assertThat(broadcaster.subscriberCount()).isZero();
    }

    @Test
    void timedOutSubscribersAreRemoved() {
        CallbackEmitter emitter = new CallbackEmitter();
        broadcaster.subscribe(emitter);

        emitter.timeout.run();
        assertThat(broadcaster.subscriberCount()).isZero();
    }

    @Test
    void weatherUpdatesReachOperatorSubscribersOnly() {
        RecordingEmitter guest = new RecordingEmitter();
        RecordingEmitter operator = new RecordingEmitter();
        broadcaster.subscribe(guest);
        broadcaster.subscribeOperator(operator);

        WeatherRecommendationOperationalUpdate weather = sampleWeather();
        AttractionOperationalUpdate attraction = sampleUpdate("evt-weather-attr", 25, 1L);
        GuestAdvisoryOperationalUpdate advisory = sampleAdvisory();
        broadcaster.publish(weather);
        broadcaster.publish(attraction);
        broadcaster.publish(advisory);

        assertThat(operator.weatherPayloads).containsExactly(weather);
        assertThat(guest.weatherPayloads).isEmpty();
        assertThat(operator.payloads).containsExactly(attraction);
        assertThat(guest.payloads).containsExactly(attraction);
        assertThat(guest.advisoryPayloads).containsExactly(advisory);
        assertThat(operator.advisoryPayloads).isEmpty();

        IncidentOperationalUpdate incident = sampleIncident();
        broadcaster.publish(incident);
        assertThat(operator.incidentPayloads).containsExactly(incident);
        assertThat(guest.incidentPayloads).isEmpty();
        assertThat(broadcaster.subscriberCount()).isEqualTo(1);
        assertThat(broadcaster.operatorSubscriberCount()).isEqualTo(1);
    }

    @Test
    void heartbeatsDoNotRemoveHealthySubscribers() {
        RecordingEmitter healthy = new RecordingEmitter();
        RecordingEmitter failing = new RecordingEmitter(new IOException("disconnected"));
        broadcaster.subscribe(healthy);
        broadcaster.subscribe(failing);

        broadcaster.sendHeartbeats();

        assertThat(healthy.comments).contains("heartbeat");
        assertThat(broadcaster.subscriberCount()).isEqualTo(1);
    }

    private static AttractionOperationalUpdate sampleUpdate(String eventId, int waitMinutes, long version) {
        return new AttractionOperationalUpdate(
                eventId,
                AttractionUpdateEventType.WAIT_TIME_CHANGED,
                NOW,
                new AttractionOperationalSnapshot(
                        "mangrove-run",
                        AttractionStatus.OPERATING,
                        CapacityMode.NORMAL,
                        waitMinutes,
                        null,
                        NOW,
                        version
                )
        );
    }

    private static WeatherRecommendationOperationalUpdate sampleWeather() {
        return new WeatherRecommendationOperationalUpdate(
                "weather-evt-1",
                WeatherRecommendationUpdateEventType.UPDATED,
                NOW,
                new WeatherRecommendationOperationalSnapshot(
                        "rec-lightning-1",
                        "simulated-lightning-hold",
                        WeatherRecommendationStatus.ACTIVE,
                        WeatherRecommendationOperatorStatus.PENDING,
                        WeatherRecommendationSeverity.WARNING,
                        "Place Mangrove Run on weather hold",
                        "Simulated lightning nearby.",
                        "Place Mangrove Run on weather hold",
                        List.of("mangrove-run"),
                        NOW,
                        NOW,
                        NOW,
                        1L,
                        1L,
                        true,
                        null
                )
        );
    }

    private static IncidentOperationalUpdate sampleIncident() {
        return new IncidentOperationalUpdate(
                "incident-evt-1",
                IncidentUpdateEventType.INCIDENT_UPDATED,
                NOW,
                new IncidentOperationalSnapshot(
                        "inc-1",
                        "Lightning activity",
                        IncidentType.WEATHER,
                        IncidentSeverity.MAJOR,
                        IncidentStatus.ACKNOWLEDGED,
                        null,
                        false,
                        null,
                        null,
                        List.of("mangrove-run"),
                        NOW,
                        2L
                )
        );
    }

    private static GuestAdvisoryOperationalUpdate sampleAdvisory() {
        return new GuestAdvisoryOperationalUpdate(
                "advisory-evt-1",
                GuestAdvisoryUpdateEventType.ADVISORY_PUBLISHED,
                NOW,
                new GuestAdvisoryOperationalSnapshot(
                        "inc-1",
                        IncidentSeverity.MAJOR,
                        "Weather advisory",
                        "Some outdoor attractions are temporarily paused.",
                        List.of("mangrove-run"),
                        NOW,
                        3L
                )
        );
    }

    private static class RecordingEmitter extends SseEmitter {
        private final List<Object> payloads = new CopyOnWriteArrayList<>();
        private final List<Object> weatherPayloads = new CopyOnWriteArrayList<>();
        private final List<Object> advisoryPayloads = new CopyOnWriteArrayList<>();
        private final List<Object> incidentPayloads = new CopyOnWriteArrayList<>();
        private final List<String> comments = new CopyOnWriteArrayList<>();
        private final IOException failure;

        RecordingEmitter() {
            this(null);
        }

        RecordingEmitter(IOException failure) {
            super(0L);
            this.failure = failure;
        }

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            if (failure != null) {
                throw failure;
            }
            for (ResponseBodyEmitter.DataWithMediaType part : builder.build()) {
                Object data = part.getData();
                if (data instanceof AttractionOperationalUpdate update) {
                    payloads.add(update);
                } else if (data instanceof WeatherRecommendationOperationalUpdate update) {
                    weatherPayloads.add(update);
                } else if (data instanceof GuestAdvisoryOperationalUpdate update) {
                    advisoryPayloads.add(update);
                } else if (data instanceof IncidentOperationalUpdate update) {
                    incidentPayloads.add(update);
                } else if (data instanceof String text && text.startsWith(":")) {
                    comments.add(text.replace(":", "").trim());
                }
            }
        }
    }

    private static final class CallbackEmitter extends SseEmitter {
        private Runnable completion;
        private Runnable timeout;

        CallbackEmitter() {
            super(0L);
        }

        @Override
        public synchronized void onCompletion(Runnable callback) {
            this.completion = callback;
            super.onCompletion(callback);
        }

        @Override
        public synchronized void onTimeout(Runnable callback) {
            this.timeout = callback;
            super.onTimeout(callback);
        }
    }
}
