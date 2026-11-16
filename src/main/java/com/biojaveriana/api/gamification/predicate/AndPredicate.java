package com.biojaveriana.api.gamification.predicate;

/**
 * AND composite predicate — satisfied when both children are satisfied.
 */
public class AndPredicate extends CompositePredicate {

    public AndPredicate(Predicate left, Predicate right) {
        super(left, right);
    }

    @Override
    public boolean evaluate(Context context) {
        return left.evaluate(context) && right.evaluate(context);
    }
}
