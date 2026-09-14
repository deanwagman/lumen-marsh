package com.deanwagman.lumenmarsh.venueops.security;

import java.util.function.Supplier;

/**
 * Request-scoped bridge so persistence can record verified identity fields
 * while domain activity still carries a display label.
 */
public final class ActorAuditContext {

    private static final ThreadLocal<ActorIdentity> CURRENT = new ThreadLocal<>();

    private ActorAuditContext() {
    }

    public static ActorIdentity currentOrLegacy(String actorLabel) {
        ActorIdentity current = CURRENT.get();
        return current != null ? current : ActorIdentity.legacy(actorLabel);
    }

    public static <T> T call(ActorIdentity identity, Supplier<T> action) {
        ActorIdentity previous = CURRENT.get();
        CURRENT.set(identity);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static void run(ActorIdentity identity, Runnable action) {
        call(identity, () -> {
            action.run();
            return null;
        });
    }
}
