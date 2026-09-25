package com.eventsphere.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for errors that should reach the client with a specific HTTP status and a readable message.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
