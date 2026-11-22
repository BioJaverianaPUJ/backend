package com.biojaveriana.api.gamification.predicate.leaf;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.biojaveriana.api.gamification.predicate.LeafPredicate;
import com.biojaveriana.api.gamification.predicate.Predicate;

/**
 * Checks if the user has a specific count of unique POIs visited.
 */
@Component
public class PoiVisitedPredicate implements LeafPredicate {
    public static final String TYPE = "poi_visited_count";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean evaluate(Predicate.Context context) {
        Map<String, Object> args = getArgs(context);
        Long count = getPoiCount(context);
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

    private Long getPoiCount(Predicate.Context context) {
        Map<String, Object> args = getArgs(context);
        if (args != null && args.containsKey("poi_visited_count")) {
            return ((Number) args.get("poi_visited_count")).longValue();
        }
        return (Long) context.get("poi_visited_count");
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

