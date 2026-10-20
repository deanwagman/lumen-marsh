package com.deanwagman.lumenmarsh.venueops.demo;

import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class DemoLeaseService {
    static final String COOKIE = "__Host-lumen-demo";

    record Issued(String token, DemoLeaseStore.Lease lease, Map<String, String> reset) {
    }

    private record RateWindow(long minute, AtomicInteger count) {
    }

    private final DemoSandboxProperties properties;
    private final DemoLeaseStore leases;
    private final DemoResetService resets;
    private final Clock clock;
    private final ConcurrentHashMap<String, RateWindow> rates = new ConcurrentHashMap<>();

    DemoLeaseService(
            DemoSandboxProperties properties,
            DemoLeaseStore leases,
            DemoResetService resets,
            Clock clock
    ) {
        this.properties = properties;
        this.leases = leases;
        this.resets = resets;
        this.clock = clock;
    }

    Issued acquire() {
        String token = DemoTokens.issue();
        String hash = DemoTokens.hash(token);
        Instant now = Instant.now(clock);
        if (leases.reserve(hash, now, properties).isEmpty()) {
            throw new LeaseUnavailableException();
        }
        try {
            Map<String, String> reset = resets.resetAll(UUID.randomUUID());
            return new Issued(token, leases.activate(hash, Instant.now(clock), properties), reset);
        } catch (RuntimeException failure) {
            leases.fail(hash, "Sandbox preparation failed.");
            throw failure;
        }
    }

    Optional<DemoLeaseStore.Lease> current(String token, boolean touch) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return leases.authenticate(DemoTokens.hash(token), Instant.now(clock), properties, touch);
    }

    DemoLeaseStore.Lease require(String token, boolean mutation) {
        String hash = token == null || token.isBlank() ? "" : DemoTokens.hash(token);
        DemoLeaseStore.Lease lease = leases.authenticate(hash, Instant.now(clock), properties, mutation)
                .orElseThrow(InvalidLeaseException::new);
        if (mutation) {
            enforceRate(hash);
        }
        return lease;
    }

    Map<String, String> reset(String token) {
        String hash = token == null || token.isBlank() ? "" : DemoTokens.hash(token);
        require(token, true);
        if (!leases.beginReset(hash, Instant.now(clock), properties)) {
            throw new InvalidLeaseException();
        }
        try {
            Map<String, String> outcome = resets.resetAll(UUID.randomUUID());
            leases.finishReset(hash, Instant.now(clock), properties);
            return outcome;
        } catch (RuntimeException failure) {
            leases.fail(hash, "Reset failed. Retry by acquiring the sandbox again.");
            throw failure;
        }
    }

    void release(String token) {
        String hash = token == null || token.isBlank() ? "" : DemoTokens.hash(token);
        require(token, true);
        if (!leases.beginReset(hash, Instant.now(clock), properties)) {
            throw new InvalidLeaseException();
        }
        try {
            resets.resetAll(UUID.randomUUID());
        } finally {
            leases.expire(hash);
            rates.remove(hash);
        }
    }

    DemoLeaseStore.Lease snapshot() {
        return leases.find();
    }

    @Scheduled(fixedDelayString = "${venueops.demo-sandbox.expiry-scan:PT15S}")
    void expireIdleLease() {
        DemoLeaseStore.Lease lease = leases.find();
        Instant now = Instant.now(clock);
        if ((lease.state() == DemoLeaseStore.State.ACTIVE
                || lease.state() == DemoLeaseStore.State.PREPARING
                || lease.state() == DemoLeaseStore.State.RESETTING)
                && !lease.liveAt(now, properties)
                && lease.tokenHash() != null) {
            if (!leases.beginExpiryReset(lease.tokenHash(), now)) {
                return;
            }
            try {
                resets.resetAll(UUID.randomUUID());
            } catch (RuntimeException ignored) {
                // Expiry must revoke authority even when a best-effort cleanup component is unavailable.
            } finally {
                leases.expire(lease.tokenHash());
                rates.remove(lease.tokenHash());
            }
        }
    }

    private void enforceRate(String hash) {
        long minute = Instant.now(clock).getEpochSecond() / 60;
        RateWindow window = rates.compute(hash, (ignored, existing) ->
                existing == null || existing.minute() != minute
                        ? new RateWindow(minute, new AtomicInteger())
                        : existing
        );
        if (window.count().incrementAndGet() > properties.mutationsPerMinute()) {
            throw new DemoRateLimitException();
        }
    }

    static final class LeaseUnavailableException extends RuntimeException {
    }

    static final class InvalidLeaseException extends RuntimeException {
    }

    static final class DemoRateLimitException extends RuntimeException {
    }
}
