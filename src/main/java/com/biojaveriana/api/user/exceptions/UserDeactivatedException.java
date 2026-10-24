package com.biojaveriana.api.user.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class UserDeactivatedException extends ServiceException {
    public UserDeactivatedException() {
        super(HttpStatus.FORBIDDEN, "USER_DEACTIVATED", "You have been banned from accessing this resource");
    }
}
