package com.biojaveriana.api.observation.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class ObservationJobNotFoundException extends ServiceException {
    public ObservationJobNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "Observation job not found with id: " + id);
    }

    public ObservationJobNotFoundException(UUID observationId, String message) {
        super(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", message);
    }
}
