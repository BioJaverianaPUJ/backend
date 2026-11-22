package com.biojaveriana.api.motd.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class FunFactNotFoundException extends ServiceException {
    public FunFactNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "FUN_FACT_NOT_FOUND", "Fun fact not found with id: " + id);
    }
}
