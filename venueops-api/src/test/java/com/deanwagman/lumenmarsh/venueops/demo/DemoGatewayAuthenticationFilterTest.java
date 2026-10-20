package com.deanwagman.lumenmarsh.venueops.demo;

import com.deanwagman.lumenmarsh.venueops.security.VenueOpsSecurityProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoGatewayAuthenticationFilterTest {

    @Test
    void requiresHttpsIssuerWhenSandboxStarts() {
        assertThatThrownBy(() -> new DemoGatewayAuthenticationFilter(
                demoProperties(),
                securityProperties("http://demo.localhost:8088")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must use HTTPS");
    }

    @Test
    void acceptsConfiguredHttpsIssuerForPublicOrLocalHosts() {
        assertThatCode(() -> new DemoGatewayAuthenticationFilter(
                demoProperties(),
                securityProperties("https://demo.localhost:8443")
        )).doesNotThrowAnyException();
        assertThatCode(() -> new DemoGatewayAuthenticationFilter(
                demoProperties(),
                securityProperties("https://demo.example.com")
        )).doesNotThrowAnyException();
    }

    private static DemoSandboxProperties demoProperties() {
        return new DemoSandboxProperties(
                true,
                "0123456789abcdef0123456789abcdef",
                "",
                "",
                "",
                "",
                "",
                Duration.ofMinutes(10),
                Duration.ofMinutes(3),
                Duration.ofMinutes(15),
                30,
                true
        );
    }

    private static VenueOpsSecurityProperties securityProperties(String issuer) {
        return new VenueOpsSecurityProperties(
                VenueOpsSecurityProperties.Mode.LOCAL_JWT,
                issuer,
                VenueOpsSecurityProperties.LOCAL_CONSOLE_CLIENT_ID,
                false,
                "https://demo.localhost:8443",
                120
        );
    }
}
