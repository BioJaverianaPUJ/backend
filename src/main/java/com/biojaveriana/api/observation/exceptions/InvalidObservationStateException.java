package com.biojaveriana.api.observation.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class InvalidObservationStateException extends ServiceException {
    public InvalidObservationStateException(String message) {
        super(HttpStatus.CONFLICT, "INVALID_OBSERVATION_STATE", message);
    }
}
