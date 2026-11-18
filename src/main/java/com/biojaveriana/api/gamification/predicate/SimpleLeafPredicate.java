package com.biojaveriana.api.gamification.predicate;

import java.util.Map;

/**
 * Simple leaf predicate that holds the type and arguments for evaluation.
 * Delegates actual evaluation to the LeafPredicateRegistry for type-specific
 * implementation lookup.
 */
public class SimpleLeafPredicate implements LeafPredicate {

    private final String type;
    private final Map<String, Object> args;
    private final LeafPredicateRegistry registry;

    public SimpleLeafPredicate(String type, Map<String, Object> args, LeafPredicateRegistry registry) {
        this.type = type;
        this.args = args;
        this.registry = registry;
    }

    @Override
    public String getType() {
        return type;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    @Override
    public boolean evaluate(Predicate.Context context) {
        // Delegate to the registry to find the actual predicate implementation
        // and evaluate it with the merged context (args + original context)
        return registry.get(type)
                .map(predicate -> {
                    // Merge args into context for evaluation
                    Predicate.Context mergedContext = new Predicate.Context() {
                        @Override
                        public Object get(String key) {
                            Object argValue = args.get(key);
                            if (argValue != null) {
                                return argValue;
                            }
                            return context.get(key);
                        }
                    };
                    return predicate.evaluate(mergedContext);
                })
                .orElse(false);
    }
}
