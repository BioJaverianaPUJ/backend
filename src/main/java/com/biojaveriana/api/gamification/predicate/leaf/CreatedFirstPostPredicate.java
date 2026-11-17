package com.biojaveriana.api.gamification.predicate.leaf;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.biojaveriana.api.gamification.predicate.LeafPredicate;
import com.biojaveriana.api.gamification.predicate.MapContext;
import com.biojaveriana.api.gamification.predicate.Predicate;

/**
 * Checks if the user has created at least one post.
 */
@Component
public class CreatedFirstPostPredicate implements LeafPredicate {

    public static final String TYPE = "created_first_post";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean evaluate(Predicate.Context context) {
        Long postCount = getPostCount(context);
        return postCount != null && postCount >= 1;
    }

    private Long getPostCount(Predicate.Context context) {
        return (Long) context.get("post_count");
    }
}

