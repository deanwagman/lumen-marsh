package com.deanwagman.lumenmarsh.venueops.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

/**
 * Structured authn/authz logs. Never logs tokens, Authorization headers, or exception messages.
 */
final class SecurityAccessLogger {

    private static final Logger log = LoggerFactory.getLogger("venueops.security");

    private SecurityAccessLogger() {
    }

    static void authenticationFailure(HttpServletRequest request, Exception ex) {
        log.warn(
                "event=authentication_failure status=401 method={} path={} exception={}",
                request.getMethod(),
                path(request),
                ex.getClass().getSimpleName()
        );
    }

    static void authorizationFailure(HttpServletRequest request, Authentication authentication, Exception ex) {
        log.warn(
                "event=authorization_failure status=403 method={} path={} subject={} exception={}",
                request.getMethod(),
                path(request),
                subject(authentication),
                ex.getClass().getSimpleName()
        );
    }

    static void rateLimited(HttpServletRequest request) {
        log.warn(
                "event=rate_limit_exceeded status=429 method={} path={}",
                request.getMethod(),
                path(request)
        );
    }

    private static String path(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || uri.isBlank() ? "/" : uri;
    }

    private static String subject(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "anonymous";
        }
        return authentication.getName();
    }
}
