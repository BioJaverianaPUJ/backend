package com.biojaveriana.api.observation.exceptions;

import org.springframework.http.HttpStatus;
import com.biojaveriana.api.common.exceptions.ServiceException;

public class InvalidObservationException extends ServiceException {
    public InvalidObservationException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_OBSERVATION", message);
    }
}
