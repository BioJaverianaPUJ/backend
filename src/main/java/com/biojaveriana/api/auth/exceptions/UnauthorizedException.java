package com.biojaveriana.api.auth.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class UnauthorizedException extends ServiceException {
    public UnauthorizedException() {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Unauthorized access");
    }

}
