package com.biojaveriana.api.observation.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class ObservationForbiddenException extends ServiceException {
    public ObservationForbiddenException() {
        super(HttpStatus.FORBIDDEN, "OBSERVATION_FORBIDDEN",
              "You can only access your own observations");
    }
}
