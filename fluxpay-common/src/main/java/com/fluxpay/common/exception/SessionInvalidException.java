package com.fluxpay.common.exception;

public class SessionInvalidException extends RuntimeException {

    public SessionInvalidException(String message) {
        super(message);
    }

    public SessionInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}

