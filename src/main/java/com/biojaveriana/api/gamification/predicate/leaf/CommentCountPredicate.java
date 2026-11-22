package com.biojaveriana.api.gamification.predicate.leaf;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.biojaveriana.api.gamification.predicate.LeafPredicate;
import com.biojaveriana.api.gamification.predicate.Predicate;

/**
 * Checks if the user has a specific count of comments.
 */
@Component
public class CommentCountPredicate implements LeafPredicate {
    public static final String TYPE = "comment_count";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean evaluate(Predicate.Context context) {
        Map<String, Object> args = getArgs(context);
        Long count = getCommentCount(context);
        if (count == null) {
            return false;
        }

        Integer target = getTarget(args);
        String operator = getOperator(args);

        return evaluateOperator(count, target, operator);
    }

    private Map<String, Object> getArgs(Predicate.Context context) {
        return (Map<String, Object>) context.get("predicate_args");
    }

    private Long getCommentCount(Predicate.Context context) {
        Map<String, Object> args = getArgs(context);
        if (args != null && args.containsKey("comment_count")) {
            return ((Number) args.get("comment_count")).longValue();
        }
        return (Long) context.get("comment_count");
    }

    private Integer getTarget(Map<String, Object> args) {
        if (args == null) return null;
        Object target = args.get("target");
        if (target instanceof Number) {
            return ((Number) target).intValue();
        }
        return null;
    }

    private String getOperator(Map<String, Object> args) {
        if (args == null) return "eq";
        return (String) args.getOrDefault("operator", "eq");
    }

    private boolean evaluateOperator(Long count, Integer target, String operator) {
        if (target == null) return false;
        return switch (operator) {
            case "eq" -> count.equals((long) target);
            case "gte" -> count >= target;
            case "gt" -> count > target;
            case "lte" -> count <= target;
            case "lt" -> count < target;
            default -> false;
        };
    }
}

