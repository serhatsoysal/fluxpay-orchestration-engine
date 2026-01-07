package com.fluxpay.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(String.format("%s not found with id: %s", resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

