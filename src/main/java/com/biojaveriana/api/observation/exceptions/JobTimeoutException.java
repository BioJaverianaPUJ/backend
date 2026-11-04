package com.biojaveriana.api.observation.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class JobTimeoutException extends ServiceException {
    public JobTimeoutException(String message) {
        super(HttpStatus.GATEWAY_TIMEOUT, "JOB_TIMEOUT", message);
    }
}
