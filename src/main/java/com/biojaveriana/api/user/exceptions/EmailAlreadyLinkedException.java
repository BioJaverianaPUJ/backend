package com.biojaveriana.api.user.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class EmailAlreadyLinkedException extends ServiceException {
    public EmailAlreadyLinkedException(String email) {
        super(HttpStatus.CONFLICT, "EMAIL_ALREADY_LINKED", "There is already an account associated with the email: " + email);
    }
}
