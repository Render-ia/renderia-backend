package com.renderia.renderia_backend.exception;

import org.springframework.http.HttpStatus;

/** 429: el cliente debe esperar retryAfterSeconds antes de volver a intentar. */
public class TooManyRequestsException extends ApiException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
