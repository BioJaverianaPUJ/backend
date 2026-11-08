package com.biojaveriana.api.comment.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class CommentForbiddenException extends ServiceException {
    public CommentForbiddenException() {
        super(HttpStatus.FORBIDDEN, "COMMENT_FORBIDDEN", "You do not have permission to perform this action");
    }
}
