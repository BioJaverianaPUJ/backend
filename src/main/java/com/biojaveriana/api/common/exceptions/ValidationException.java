package com.biojaveriana.api.common.exceptions;

import org.springframework.http.HttpStatus;

public class ValidationException extends ServiceException {
    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

}
