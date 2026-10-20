package com.deanwagman.lumenmarsh.venueops.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VenueOpsSecurityPropertiesTest {

    @Test
    void rejectsIssuerThatIsNotAnAbsoluteHttpUrl() {
        assertThatThrownBy(() -> new VenueOpsSecurityProperties(
                VenueOpsSecurityProperties.Mode.LOCAL_JWT,
                "lumen-marsh-demo",
                VenueOpsSecurityProperties.LOCAL_CONSOLE_CLIENT_ID,
                false,
                "http://localhost:*",
                120
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("absolute HTTP(S) URL");
    }
}
