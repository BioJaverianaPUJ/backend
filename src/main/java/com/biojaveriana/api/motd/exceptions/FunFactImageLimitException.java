package com.biojaveriana.api.motd.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class FunFactImageLimitException extends ServiceException {
    public FunFactImageLimitException(int maxImages) {
        super(HttpStatus.BAD_REQUEST, "FUN_FACT_IMAGE_LIMIT_EXCEEDED", "Maximum number of images is " + maxImages);
    }
}
