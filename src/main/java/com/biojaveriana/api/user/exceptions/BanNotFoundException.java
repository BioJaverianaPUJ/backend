package com.biojaveriana.api.user.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class BanNotFoundException extends ServiceException {
    public BanNotFoundException(UUID banId) {
        super(HttpStatus.NOT_FOUND, "BAN_NOT_FOUND", "Ban not found with ID: " + banId);
    }
}