package com.biojaveriana.api.post.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class PostNotFoundException extends ServiceException {
    public PostNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Post not found with id: " + id);
    }
}
