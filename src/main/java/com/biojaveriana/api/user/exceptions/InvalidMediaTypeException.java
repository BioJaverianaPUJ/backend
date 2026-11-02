package com.biojaveriana.api.user.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class InvalidMediaTypeException extends ServiceException {
    public InvalidMediaTypeException(String mediaType) {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
              "INVALID_MEDIA_TYPE",
              "Invalid media type: " + mediaType);
    }
}
