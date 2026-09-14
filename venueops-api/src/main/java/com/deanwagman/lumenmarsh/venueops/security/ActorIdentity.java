package com.deanwagman.lumenmarsh.venueops.security;

import java.util.Objects;

/**
 * Verified audit identity derived from credentials — never from client-supplied headers.
 */
public record ActorIdentity(
        String subject,
        String displayName,
        ActorType type,
        String issuer
) {
    public static final String SYSTEM_SUBJECT = "venueops-system";
    public static final String SYSTEM_ISSUER = "venueops";
    public static final String WEATHER_SERVICE_DISPLAY = "environmental-monitor";

    public ActorIdentity {
        Objects.requireNonNull(subject, "subject is required");
        Objects.requireNonNull(displayName, "displayName is required");
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(issuer, "issuer is required");
        if (subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }

    public static ActorIdentity system() {
        return new ActorIdentity(SYSTEM_SUBJECT, "VenueOps system", ActorType.SYSTEM, SYSTEM_ISSUER);
    }

    public static ActorIdentity legacy(String priorActor) {
        String value = priorActor == null || priorActor.isBlank() ? "unknown" : priorActor;
        return new ActorIdentity(value, value, ActorType.LEGACY, SYSTEM_ISSUER);
    }

    /** Label persisted in the legacy {@code actor} column and returned to clients. */
    public String auditLabel() {
        return displayName;
    }
}
