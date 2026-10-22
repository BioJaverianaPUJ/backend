package com.biojaveriana.api.common.exceptions;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public abstract class ServiceException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String description;

    protected ServiceException(HttpStatus status, String code, String description) {
        super(description);

        this.status = status;
        this.code = code;
        this.description = description;
    }
}
