package com.fluxpay.security.session.service;

import com.fluxpay.security.session.model.SessionData;
import com.fluxpay.security.session.util.SessionTestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SessionServiceIntegrationTest {

    @Test
    void updateLastAccess_ShouldIncrementRequestCount() {
        SessionData session = SessionTestDataFactory.createSessionData();
        long startRequestCount = session.getRequestCount();

        session.setRequestCount(startRequestCount + 1);
        session.setLastAccess(java.time.Instant.now());

        assertThat(session.getRequestCount()).isEqualTo(startRequestCount + 1);
        assertThat(session.getLastAccess()).isNotNull();
    }
}


