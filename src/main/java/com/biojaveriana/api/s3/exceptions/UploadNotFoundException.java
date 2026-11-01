package com.biojaveriana.api.s3.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class UploadNotFoundException extends ServiceException {
    public UploadNotFoundException() {
        super(HttpStatus.NOT_FOUND, "OBJECT_EXPIRED", "Object not found in storage. Maybe expired?");
    }
}
