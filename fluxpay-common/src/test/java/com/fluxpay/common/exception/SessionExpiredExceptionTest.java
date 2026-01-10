package com.fluxpay.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionExpiredExceptionTest {

    @Test
    void constructor_WithMessage_Success() {
        String message = "Session has expired";
        SessionExpiredException exception = new SessionExpiredException(message);

        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructor_WithMessageAndCause_Success() {
        String message = "Session has expired";
        Throwable cause = new RuntimeException("Root cause");
        SessionExpiredException exception = new SessionExpiredException(message, cause);

        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}

