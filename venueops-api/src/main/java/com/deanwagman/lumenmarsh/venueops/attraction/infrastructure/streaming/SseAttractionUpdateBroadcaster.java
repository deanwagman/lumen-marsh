package com.deanwagman.lumenmarsh.venueops.attraction.infrastructure.streaming;

import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.attraction.application.AttractionUpdatePublisher;
import com.deanwagman.lumenmarsh.venueops.incident.application.GuestAdvisoryOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.incident.application.GuestAdvisoryUpdatePublisher;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.incident.application.IncidentUpdatePublisher;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationOperationalUpdate;
import com.deanwagman.lumenmarsh.venueops.weather.application.WeatherRecommendationUpdatePublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseAttractionUpdateBroadcaster
        implements AttractionUpdatePublisher, GuestAdvisoryUpdatePublisher, WeatherRecommendationUpdatePublisher, IncidentUpdatePublisher {

    public static final String UPDATE_EVENT = "attraction.updated";
    public static final String SNAPSHOT_EVENT = "attractions.snapshot";
    public static final String ADVISORIES_SNAPSHOT_EVENT = "advisories.snapshot";

    private final ConcurrentHashMap<String, SseEmitter> guestEmitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SseEmitter> operatorEmitters = new ConcurrentHashMap<>();
    private final long timeoutMs;
    private final long reconnectMs;
    private final Counter updatesPublished;
    private final Counter failedSends;
    private final Counter subscribersDisconnected;
    private final Counter snapshotFailures;

    public SseAttractionUpdateBroadcaster(
            MeterRegistry meterRegistry,
            @Value("${venueops.attractions.sse.timeout-ms:1800000}") long timeoutMs,
            @Value("${venueops.attractions.sse.reconnect-ms:3000}") long reconnectMs
    ) {
        this.timeoutMs = timeoutMs;
        this.reconnectMs = reconnectMs;
        this.updatesPublished = meterRegistry.counter("venueops.attractions.sse.updates.published");
        this.failedSends = meterRegistry.counter("venueops.attractions.sse.sends.failed");
        this.subscribersDisconnected = meterRegistry.counter("venueops.attractions.sse.subscribers.disconnected");
        this.snapshotFailures = meterRegistry.counter("venueops.attractions.sse.snapshot.failures");
        Gauge.builder("venueops.attractions.sse.subscribers", guestEmitters, ConcurrentHashMap::size)
                .register(meterRegistry);
        Gauge.builder("venueops.operator.sse.subscribers", operatorEmitters, ConcurrentHashMap::size)
                .register(meterRegistry);
    }

    public SseEmitter subscribe() {
        return subscribe(guestEmitters, new SseEmitter(timeoutMs));
    }

    public SseEmitter subscribeOperator() {
        return subscribe(operatorEmitters, new SseEmitter(timeoutMs));
    }

    SseEmitter subscribe(SseEmitter emitter) {
        return subscribe(guestEmitters, emitter);
    }

    SseEmitter subscribeOperator(SseEmitter emitter) {
        return subscribe(operatorEmitters, emitter);
    }

    private SseEmitter subscribe(ConcurrentHashMap<String, SseEmitter> emitters, SseEmitter emitter) {
        String id = UUID.randomUUID().toString();
        emitter.onCompletion(() -> drop(id));
        emitter.onTimeout(() -> drop(id));
        emitter.onError(error -> drop(id));
        emitters.put(id, emitter);
        return emitter;
    }

    public void sendSnapshot(SseEmitter emitter, Object catalog) {
        sendNamed(emitter, SNAPSHOT_EVENT, null, catalog);
    }

    public void sendAdvisoriesSnapshot(SseEmitter emitter, Object catalog) {
        sendNamed(emitter, ADVISORIES_SNAPSHOT_EVENT, null, catalog);
    }

    public void sendNamed(SseEmitter emitter, String eventName, String eventId, Object data) {
        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .name(eventName)
                    .reconnectTime(reconnectMs)
                    .data(data);
            if (eventId != null) {
                event.id(eventId);
            }
            emitter.send(event);
        } catch (Exception ex) {
            snapshotFailures.increment();
            drop(emitter);
        }
    }

    public void unsubscribe(SseEmitter emitter) {
        drop(emitter);
    }

    @Override
    public void publish(AttractionOperationalUpdate update) {
        broadcast(guestEmitters, update.eventId(), UPDATE_EVENT, update);
        broadcast(operatorEmitters, update.eventId(), UPDATE_EVENT, update);
    }

    @Override
    public void publish(GuestAdvisoryOperationalUpdate update) {
        broadcast(guestEmitters, update.eventId(), update.sseEventName(), update);
    }

    @Override
    public void publish(WeatherRecommendationOperationalUpdate update) {
        broadcast(operatorEmitters, update.eventId(), update.sseEventName(), update);
    }

    @Override
    public void publish(IncidentOperationalUpdate update) {
        broadcast(operatorEmitters, update.eventId(), update.sseEventName(), update);
    }

    private void broadcast(ConcurrentHashMap<String, SseEmitter> emitters, String eventId, String eventName, Object payload) {
        updatesPublished.increment();
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(eventId)
                        .name(eventName)
                        .reconnectTime(reconnectMs)
                        .data(payload));
            } catch (Exception ex) {
                failedSends.increment();
                drop(id);
            }
        });
    }

    @Scheduled(fixedRateString = "${venueops.attractions.sse.heartbeat-interval-ms:15000}")
    public void sendHeartbeats() {
        sendHeartbeats(guestEmitters);
        sendHeartbeats(operatorEmitters);
    }

    private void sendHeartbeats(ConcurrentHashMap<String, SseEmitter> emitters) {
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception ex) {
                failedSends.increment();
                drop(id);
            }
        });
    }

    int subscriberCount() {
        return guestEmitters.size();
    }

    int operatorSubscriberCount() {
        return operatorEmitters.size();
    }

    @PreDestroy
    public void completeAll() {
        List.copyOf(guestEmitters.keySet()).forEach(this::drop);
        List.copyOf(operatorEmitters.keySet()).forEach(this::drop);
    }

    private void drop(SseEmitter emitter) {
        dropMatching(guestEmitters, emitter);
        dropMatching(operatorEmitters, emitter);
    }

    private void dropMatching(ConcurrentHashMap<String, SseEmitter> emitters, SseEmitter emitter) {
        emitters.forEach((id, candidate) -> {
            if (candidate == emitter) {
                drop(id);
            }
        });
    }

    private void drop(String id) {
        SseEmitter removed = guestEmitters.remove(id);
        if (removed == null) {
            removed = operatorEmitters.remove(id);
        }
        if (removed != null) {
            subscribersDisconnected.increment();
            try {
                removed.complete();
            } catch (Exception ignored) {
                // Already closed by the client or container.
            }
        }
    }
}
