package com.biojaveriana.api.user.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class NewPasswordSameAsCurrentException extends ServiceException {
    public NewPasswordSameAsCurrentException() {
        super(HttpStatus.BAD_REQUEST, "NEW_PASSWORD_SAME_AS_CURRENT", "The new password must be different from the current password.");
    }
}
