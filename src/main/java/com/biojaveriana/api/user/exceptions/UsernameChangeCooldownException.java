package com.biojaveriana.api.user.exceptions;

import org.springframework.http.HttpStatus;
import com.biojaveriana.api.common.exceptions.ServiceException;

public class UsernameChangeCooldownException extends ServiceException {
    public UsernameChangeCooldownException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "USERNAME_CHANGE_COOLDOWN", "Username can only be changed once every two weeks. Please try again later.");
    }
}
