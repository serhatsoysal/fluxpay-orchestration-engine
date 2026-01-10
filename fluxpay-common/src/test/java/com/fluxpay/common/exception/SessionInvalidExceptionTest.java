package com.fluxpay.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionInvalidExceptionTest {

    @Test
    void constructor_WithMessage_Success() {
        String message = "Session is invalid";
        SessionInvalidException exception = new SessionInvalidException(message);

        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructor_WithMessageAndCause_Success() {
        String message = "Session is invalid";
        Throwable cause = new RuntimeException("Root cause");
        SessionInvalidException exception = new SessionInvalidException(message, cause);

        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}

