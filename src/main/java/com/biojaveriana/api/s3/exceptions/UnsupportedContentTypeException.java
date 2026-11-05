package com.biojaveriana.api.s3.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class UnsupportedContentTypeException extends ServiceException {
    public UnsupportedContentTypeException(String contentType) {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_CONTENT_TYPE", "The following content type is not supported:" + contentType);
    }
}
