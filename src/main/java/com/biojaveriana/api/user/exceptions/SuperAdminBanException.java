package com.biojaveriana.api.user.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class SuperAdminBanException extends ServiceException {
    public SuperAdminBanException() {
        super(HttpStatus.FORBIDDEN, "SUPERADMIN_CANNOT_BE_BANNED", "The superadmin account cannot be banned.");
    }
}