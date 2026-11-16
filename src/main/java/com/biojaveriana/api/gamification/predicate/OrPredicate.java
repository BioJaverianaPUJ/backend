package com.biojaveriana.api.gamification.predicate;

/**
 * OR composite predicate — satisfied when at least one child is satisfied.
 */
public class OrPredicate extends CompositePredicate {

    public OrPredicate(Predicate left, Predicate right) {
        super(left, right);
    }

    @Override
    public boolean evaluate(Context context) {
        return left.evaluate(context) || right.evaluate(context);
    }
}
