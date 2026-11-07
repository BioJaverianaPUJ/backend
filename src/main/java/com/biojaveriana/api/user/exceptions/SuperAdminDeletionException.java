package com.biojaveriana.api.user.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class SuperAdminDeletionException extends ServiceException {
    public SuperAdminDeletionException() {
        super(HttpStatus.FORBIDDEN, "SUPERADMIN_CANNOT_BE_DELETED", "The superadmin account cannot be deleted.");
    }
}
