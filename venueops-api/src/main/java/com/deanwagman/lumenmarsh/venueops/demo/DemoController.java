package com.deanwagman.lumenmarsh.venueops.demo;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/demo")
@ConditionalOnProperty(name = "venueops.demo-sandbox.enabled", havingValue = "true")
final class DemoController {
    private final DemoSandboxProperties properties;
    private final DemoLeaseService leases;
    private final DemoGatewayService gateway;
    private final Clock clock;

    DemoController(
            DemoSandboxProperties properties,
            DemoLeaseService leases,
            DemoGatewayService gateway,
            Clock clock
    ) {
        this.properties = properties;
        this.leases = leases;
        this.gateway = gateway;
        this.clock = clock;
    }

    @GetMapping("/readiness")
    DemoGatewayService.Readiness readiness() {
        return gateway.readiness();
    }

    @GetMapping("/lease")
    Map<String, Object> lease(@CookieValue(name = DemoLeaseService.COOKIE, required = false) String token) {
        Instant now = Instant.now(clock);
        DemoLeaseStore.Lease current = leases.snapshot();
        boolean holder = leases.current(token, false).isPresent();
        boolean live = current.liveAt(now, properties);
        long remaining = live && current.expiresAt() != null
                ? Math.max(0, Duration.between(now, current.expiresAt()).toSeconds())
                : 0;
        return Map.of(
                "state", live ? current.state().name() : "AVAILABLE",
                "holder", holder,
                "available", !live,
                "remainingSeconds", remaining
        );
    }

    @PostMapping("/leases")
    ResponseEntity<Map<String, Object>> acquire(HttpServletResponse response) {
        DemoLeaseService.Issued issued = leases.acquire();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(issued.token(), properties.hardMaximum()).toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "state", issued.lease().state().name(),
                "holder", true,
                "remainingSeconds", Duration.between(Instant.now(clock), issued.lease().expiresAt()).toSeconds(),
                "reset", issued.reset()
        ));
    }

    @PostMapping("/leases/heartbeat")
    Map<String, Object> heartbeat(
            @CookieValue(name = DemoLeaseService.COOKIE, required = false) String token
    ) {
        DemoLeaseStore.Lease lease = leases.require(token, true);
        return Map.of(
                "state", lease.state().name(),
                "remainingSeconds", Math.max(0, Duration.between(Instant.now(clock), lease.expiresAt()).toSeconds())
        );
    }

    @DeleteMapping("/leases/current")
    ResponseEntity<Void> release(
            @CookieValue(name = DemoLeaseService.COOKIE, required = false) String token,
            HttpServletResponse response
    ) {
        leases.release(token);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset")
    Map<String, Object> reset(@CookieValue(name = DemoLeaseService.COOKIE, required = false) String token) {
        DemoLeaseService.ResetResult result = leases.reset(token);
        return Map.of(
                "state", "ACTIVE",
                "resetId", result.resetId(),
                "reset", result.reset()
        );
    }

    @PostMapping("/scenarios/{name}")
    Object scenario(
            @PathVariable String name,
            @CookieValue(name = DemoLeaseService.COOKIE, required = false) String token
    ) {
        leases.require(token, true);
        return gateway.scenario(name);
    }

    @ExceptionHandler(DemoLeaseService.LeaseUnavailableException.class)
    ResponseEntity<Map<String, Object>> unavailable() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("title", "Sandbox in use", "status", 409));
    }

    @ExceptionHandler(DemoLeaseService.InvalidLeaseException.class)
    ResponseEntity<Map<String, Object>> invalid() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("title", "Active sandbox lease required", "status", 403));
    }

    @ExceptionHandler(DemoLeaseService.DemoRateLimitException.class)
    ResponseEntity<Map<String, Object>> rateLimited() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("title", "Demo mutation rate exceeded", "status", 429));
    }

    @ExceptionHandler(DemoGatewayService.UnknownScenarioException.class)
    ResponseEntity<Map<String, Object>> unknownScenario() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("title", "Unknown demo scenario", "status", 404));
    }

    @ExceptionHandler(DemoResetService.ResetFailedException.class)
    ResponseEntity<Map<String, Object>> resetFailed(DemoResetService.ResetFailedException failure) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "title", "Sandbox reset failed",
                "status", 503,
                "component", failure.component(),
                "resetId", failure.resetId()
        ));
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(DemoLeaseService.COOKIE, value)
                .httpOnly(true)
                .secure(properties.secureCookie())
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
