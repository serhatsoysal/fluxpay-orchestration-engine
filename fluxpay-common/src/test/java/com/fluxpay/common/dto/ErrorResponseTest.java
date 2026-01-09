package com.fluxpay.common.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void testOf_ShouldCreateErrorResponseWithAllFields() {
        String message = "Test error";
        String path = "/api/test";
        
        ErrorResponse response = ErrorResponse.of(400, "Bad Request", message, path);
        
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getError()).isEqualTo("Bad Request");
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getPath()).isEqualTo(path);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getTimestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void testNoArgsConstructor() {
        ErrorResponse response = new ErrorResponse();
        
        assertThat(response).isNotNull();
    }

    @Test
    void testAllArgsConstructor() {
        Instant timestamp = Instant.now();
        ErrorResponse response = new ErrorResponse(timestamp, 500, "Internal Server Error", "Error message", "/api/test");
        
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getError()).isEqualTo("Internal Server Error");
        assertThat(response.getMessage()).isEqualTo("Error message");
        assertThat(response.getPath()).isEqualTo("/api/test");
    }

    @Test
    void testSettersAndGetters() {
        ErrorResponse response = new ErrorResponse();
        Instant timestamp = Instant.now();
        
        response.setTimestamp(timestamp);
        response.setStatus(404);
        response.setError("Not Found");
        response.setMessage("Resource not found");
        response.setPath("/api/resource");
        
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getError()).isEqualTo("Not Found");
        assertThat(response.getMessage()).isEqualTo("Resource not found");
        assertThat(response.getPath()).isEqualTo("/api/resource");
    }
}

