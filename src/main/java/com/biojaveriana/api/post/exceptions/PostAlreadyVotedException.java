package com.biojaveriana.api.post.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class PostAlreadyVotedException extends ServiceException {
    public PostAlreadyVotedException() {
        super(HttpStatus.CONFLICT, "POST_ALREADY_VOTED", "You have already voted on this post");
    }
}
