package com.fluxpay.security.session.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fluxpay.security.session.util.SessionTestDataFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SessionDataTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void builder_ShouldCreateSessionData() {
        SessionData session = SessionData.builder()
                .sessionId(UUID.randomUUID().toString())
                .accessToken("token")
                .refreshToken("refresh")
                .userId(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .role("USER")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertThat(session).isNotNull();
        assertThat(session.getSessionId()).isNotNull();
        assertThat(session.getAccessToken()).isEqualTo("token");
    }

    @Test
    void shouldSerializeAndDeserialize() throws Exception {
        SessionData session = SessionTestDataFactory.createSessionData();

        String json = objectMapper.writeValueAsString(session);
        SessionData deserialized = objectMapper.readValue(json, SessionData.class);

        assertThat(deserialized.getSessionId()).isEqualTo(session.getSessionId());
        assertThat(deserialized.getUserId()).isEqualTo(session.getUserId());
        assertThat(deserialized.getTenantId()).isEqualTo(session.getTenantId());
    }

    @Test
    void setters_ShouldUpdateFields() {
        SessionData session = SessionTestDataFactory.createSessionData();

        session.setRequestCount(100);
        session.setMetadata(new HashMap<>());

        assertThat(session.getRequestCount()).isEqualTo(100);
        assertThat(session.getMetadata()).isNotNull();
    }

    @Test
    void equals_ShouldWorkCorrectly() {
        SessionData session1 = SessionTestDataFactory.createSessionData();
        SessionData session2 = SessionTestDataFactory.createSessionData();

        assertThat(session1).isNotEqualTo(session2);
        assertThat(session1).isEqualTo(session1);
    }

    @Test
    void noArgsConstructor_ShouldWork() {
        SessionData session = new SessionData();
        session.setSessionId("test");

        assertThat(session.getSessionId()).isEqualTo("test");
    }
}


