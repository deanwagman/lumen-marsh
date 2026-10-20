package com.deanwagman.lumenmarsh.venueops.demo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@RestController
@ConditionalOnProperty(name = "venueops.demo-sandbox.enabled", havingValue = "true")
final class DemoInternalResetController {
    record ResetRequest(UUID resetId) {
    }

    private final DemoSandboxProperties properties;
    private final DemoResetService resets;

    DemoInternalResetController(DemoSandboxProperties properties, DemoResetService resets) {
        this.properties = properties;
        this.resets = resets;
    }

    @PostMapping("/internal/demo/reset")
    Map<String, Object> reset(
            @RequestHeader(name = "X-Demo-Reset-Secret", required = false) String secret,
            @RequestBody ResetRequest request
    ) {
        if (secret == null || !MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                properties.internalSecret().getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (request == null || request.resetId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return Map.of(
                "resetId", request.resetId(),
                "component", "venueops",
                "status", resets.resetVenueOps(request.resetId()).name()
        );
    }
}
