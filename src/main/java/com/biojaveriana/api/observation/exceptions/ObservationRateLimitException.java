package com.biojaveriana.api.observation.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class ObservationRateLimitException extends ServiceException {
    public ObservationRateLimitException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "OBSERVATION_RATE_LIMIT_EXCEEDED",
            "You can only create one observation every " + retryAfterSeconds + " seconds. Please try again later.");
    }
}
