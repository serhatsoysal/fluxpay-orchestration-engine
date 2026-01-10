package com.fluxpay.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitExceededExceptionTest {

    @Test
    void constructor_WithMessage_Success() {
        String message = "Rate limit exceeded";
        RateLimitExceededException exception = new RateLimitExceededException(message);

        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructor_WithMessageAndCause_Success() {
        String message = "Rate limit exceeded";
        Throwable cause = new RuntimeException("Root cause");
        RateLimitExceededException exception = new RateLimitExceededException(message, cause);

        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}

