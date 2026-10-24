package com.biojaveriana.api.common.exceptions;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ServiceException {
    public ForbiddenException() {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not allowed to access this endpoint");
    }

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
