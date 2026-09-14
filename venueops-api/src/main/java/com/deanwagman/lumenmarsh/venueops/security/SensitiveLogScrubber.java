package com.deanwagman.lumenmarsh.venueops.security;

import java.util.regex.Pattern;

/**
 * Defense-in-depth redaction for log messages. Auth code must still avoid logging tokens.
 */
public final class SensitiveLogScrubber {

    private static final Pattern BEARER = Pattern.compile("(?i)(Bearer\\s+)[A-Za-z0-9._\\-+/=]+");
    private static final Pattern HEADER = Pattern.compile(
            "(?i)((?:Authorization|X-Api-Key)\\s*[=:]\\s*)(?:Bearer\\s+)?\\S+"
    );
    private static final Pattern SECRET = Pattern.compile(
            "(?i)((?:client[_-]?secret|oidc_client_secret|clientSecret)\\s*[=:]\\s*)\\S+"
    );
    private static final Pattern JWT = Pattern.compile(
            "\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b"
    );

    private SensitiveLogScrubber() {
    }

    public static String scrub(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String redacted = HEADER.matcher(value).replaceAll("$1***");
        redacted = BEARER.matcher(redacted).replaceAll("$1***");
        redacted = SECRET.matcher(redacted).replaceAll("$1***");
        return JWT.matcher(redacted).replaceAll("[redacted-jwt]");
    }
}
