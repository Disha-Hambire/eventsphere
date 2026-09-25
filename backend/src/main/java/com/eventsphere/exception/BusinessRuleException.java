package com.eventsphere.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is well-formed but breaks a business rule
 * (e.g. "Cannot publish: the event has no sessions"). The message is shown to the user as-is.
 */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
