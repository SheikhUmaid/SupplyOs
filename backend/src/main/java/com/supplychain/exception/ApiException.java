package com.supplychain.exception;

import org.springframework.http.HttpStatus;

/** An expected failure whose message is safe to show to the API client. */
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
