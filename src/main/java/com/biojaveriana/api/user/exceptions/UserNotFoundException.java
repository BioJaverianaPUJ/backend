package com.biojaveriana.api.user.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class UserNotFoundException extends ServiceException {
    public UserNotFoundException(UUID userId) {
        super(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found with ID: " + userId);
    }

    public UserNotFoundException(String username) {
        super(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found with username: " + username);
    }
}
