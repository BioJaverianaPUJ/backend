package com.biojaveriana.api.common.exceptions;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ServiceException {
    public NotFoundException() {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found");
    }

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
