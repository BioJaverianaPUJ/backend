package com.biojaveriana.api.common.exceptions;

import org.springframework.http.HttpStatus;

public class NotAuthorizedException extends ServiceException {
    public NotAuthorizedException() {
        super(HttpStatus.UNAUTHORIZED, "NOT_AUTHORIZED", "You are not authorized to access this resource");
    }

    public NotAuthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "NOT_AUTHORIZED", message);
    }
}
