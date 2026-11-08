package com.biojaveriana.api.common.exceptions;

import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public abstract class ServiceException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String description;
    private final Map<String, Object> details;

    protected ServiceException(HttpStatus status, String code, String description) {
        this(status, code, description, Collections.emptyMap());
    }

    protected ServiceException(HttpStatus status, String code, String description, Map<String, Object> details) {
        super(description);

        this.status = status;
        this.code = code;
        this.description = description;
        this.details = Collections.unmodifiableMap(details);
    }
}
