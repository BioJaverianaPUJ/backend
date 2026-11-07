package com.biojaveriana.api.comment.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.comment.ReactionType;
import com.biojaveriana.api.common.exceptions.ServiceException;

public class CommentReactionDuplicateException extends ServiceException {
    public CommentReactionDuplicateException(ReactionType emoji) {
        super(HttpStatus.CONFLICT, "COMMENT_REACTION_DUPLICATE", "You have already reacted with " + emoji + " to this comment");
    }
}
