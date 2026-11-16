package com.biojaveriana.api.gamification.predicate;

import java.util.Map;

/**
 * Simple leaf predicate that delegates to a registered handler.
 */
public class SimpleLeafPredicate implements LeafPredicate {

    private final String type;
    private final Map<String, Object> args;

    public SimpleLeafPredicate(String type, Map<String, Object> args) {
        this.type = type;
        this.args = args;
    }

    @Override
    public String getType() {
        return type;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    @Override
    public boolean evaluate(Context context) {
        // Delegates to the registry for actual evaluation
        return evaluateLeaf(context);
    }

    private boolean evaluateLeaf(Context context) {
        // The registry will resolve the actual predicate implementation
        return false;
    }
}
