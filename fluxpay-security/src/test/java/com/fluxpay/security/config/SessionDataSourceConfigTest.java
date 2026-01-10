package com.fluxpay.security.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionDataSourceConfigTest {

    @Test
    void configurationClass_Exists() {
        SessionDataSourceConfig config = new SessionDataSourceConfig();

        assertThat(config).isNotNull();
    }
}

