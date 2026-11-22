package com.biojaveriana.api.user.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class InvalidCurrentPasswordException extends ServiceException {
    public InvalidCurrentPasswordException() {
        super(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "The current password is incorrect.");
    }
}
