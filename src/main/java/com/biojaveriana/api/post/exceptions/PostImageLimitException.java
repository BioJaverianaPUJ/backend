package com.biojaveriana.api.post.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class PostImageLimitException extends ServiceException {
    public PostImageLimitException(int maxImages) {
        super(HttpStatus.BAD_REQUEST, "POST_IMAGE_LIMIT_EXCEEDED", "Maximum number of images is " + maxImages);
    }
}
