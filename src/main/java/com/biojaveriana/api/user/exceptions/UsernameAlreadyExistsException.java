package com.biojaveriana.api.user.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class UsernameAlreadyExistsException extends ServiceException {
    public UsernameAlreadyExistsException(String username) {
        super(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS", "Username already exists: " + username);
    }
}
