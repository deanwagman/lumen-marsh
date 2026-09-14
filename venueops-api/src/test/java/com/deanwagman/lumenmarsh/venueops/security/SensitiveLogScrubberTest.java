package com.deanwagman.lumenmarsh.venueops.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveLogScrubberTest {

    @Test
    void redactsAuthorizationHeaders() {
        assertThat(SensitiveLogScrubber.scrub("Authorization: Bearer abc.def.ghi extra"))
                .isEqualTo("Authorization: *** extra");
    }

    @Test
    void redactsBareBearerTokens() {
        assertThat(SensitiveLogScrubber.scrub("token Bearer abcdef extra"))
                .isEqualTo("token Bearer *** extra");
    }

    @Test
    void redactsClientSecrets() {
        assertThat(SensitiveLogScrubber.scrub("client_secret=super-secret-value"))
                .isEqualTo("client_secret=***");
    }

    @Test
    void redactsCompactJwts() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature";
        assertThat(SensitiveLogScrubber.scrub("token=" + jwt))
                .isEqualTo("token=[redacted-jwt]");
    }

    @Test
    void leavesOrdinaryMessagesAlone() {
        assertThat(SensitiveLogScrubber.scrub("event=authentication_failure status=401"))
                .isEqualTo("event=authentication_failure status=401");
    }
}
