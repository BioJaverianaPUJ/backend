package com.biojaveriana.api.common.exceptions;

import org.springframework.http.HttpStatus;

public class MethodNotAllowedException extends ServiceException {
    public MethodNotAllowedException() {
        super(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "The HTTP method is not allowed for this endpoint");
    }

    public MethodNotAllowedException(String allowedMethods) {
        super(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "The HTTP method is not allowed. Allowed methods: " + allowedMethods);
    }
}
