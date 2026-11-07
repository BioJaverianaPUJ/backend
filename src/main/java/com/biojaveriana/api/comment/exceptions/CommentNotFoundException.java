package com.biojaveriana.api.comment.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class CommentNotFoundException extends ServiceException {
    public CommentNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "Comment not found with id: " + id);
    }
}
