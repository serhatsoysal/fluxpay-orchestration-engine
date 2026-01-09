package com.fluxpay.security.session.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SessionPropertiesTest {

    @Test
    void shouldHaveDefaultValues() {
        SessionProperties properties = new SessionProperties();

        assertThat(properties.getTtl().getAccessToken()).isEqualTo(Duration.ofHours(1));
        assertThat(properties.getTtl().getRefreshToken()).isEqualTo(Duration.ofDays(30));
        assertThat(properties.getConcurrent().getMaxSessions()).isEqualTo(5);
        assertThat(properties.getSecurity().isFingerprintVerification()).isTrue();
        assertThat(properties.getAudit().getRetentionDays()).isEqualTo(365);
    }

    @Test
    void setters_ShouldUpdateValues() {
        SessionProperties properties = new SessionProperties();
        properties.getTtl().setAccessToken(Duration.ofHours(2));
        properties.getConcurrent().setMaxSessions(10);

        assertThat(properties.getTtl().getAccessToken()).isEqualTo(Duration.ofHours(2));
        assertThat(properties.getConcurrent().getMaxSessions()).isEqualTo(10);
    }
}


