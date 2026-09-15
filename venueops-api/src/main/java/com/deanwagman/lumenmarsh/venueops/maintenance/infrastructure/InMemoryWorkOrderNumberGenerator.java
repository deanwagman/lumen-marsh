package com.deanwagman.lumenmarsh.venueops.maintenance.infrastructure;

import com.deanwagman.lumenmarsh.venueops.maintenance.application.WorkOrderNumberGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnProperty(name = "venueops.attractions.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryWorkOrderNumberGenerator implements WorkOrderNumberGenerator {

    private final Map<Integer, AtomicLong> sequences = new ConcurrentHashMap<>();

    @Override
    public String next(Clock clock) {
        int year = clock.instant().atZone(ZoneOffset.UTC).getYear();
        long value = sequences.computeIfAbsent(year, ignored -> new AtomicLong(0)).incrementAndGet();
        return "LM-%d-%04d".formatted(year, value);
    }
}
