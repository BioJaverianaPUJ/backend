package com.biojaveriana.api.user.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class SuperAdminRoleModificationException extends ServiceException {
    public SuperAdminRoleModificationException() {
        super(HttpStatus.FORBIDDEN, "SUPERADMIN_ROLE_CANNOT_BE_MODIFIED", "The superadmin role cannot be modified.");
    }
}
