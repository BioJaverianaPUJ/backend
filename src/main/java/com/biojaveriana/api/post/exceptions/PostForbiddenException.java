package com.biojaveriana.api.post.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class PostForbiddenException extends ServiceException {
    public PostForbiddenException() {
        super(HttpStatus.FORBIDDEN, "POST_FORBIDDEN", "You do not have permission to perform this action");
    }
}
