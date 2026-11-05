package com.biojaveriana.api.observation.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class ObservationNotFoundException extends ServiceException {
    public ObservationNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "OBSERVATION_NOT_FOUND", "Observation not found with id: " + id);
    }
}
