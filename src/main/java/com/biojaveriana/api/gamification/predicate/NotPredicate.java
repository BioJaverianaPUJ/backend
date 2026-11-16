package com.biojaveriana.api.gamification.predicate;

/**
 * NOT predicate — satisfied when the child is NOT satisfied.
 */
public class NotPredicate implements Predicate {

    private final Predicate child;

    public NotPredicate(Predicate child) {
        this.child = child;
    }

    public Predicate getChild() {
        return child;
    }

    @Override
    public boolean evaluate(Context context) {
        return !child.evaluate(context);
    }
}
