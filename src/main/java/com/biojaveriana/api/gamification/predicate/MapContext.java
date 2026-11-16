package com.biojaveriana.api.gamification.predicate;

import java.util.Map;

/**
 * Simple map-based context implementation for predicate evaluation.
 */
public class MapContext implements Predicate.Context {

    private final Map<String, Object> map;

    public MapContext(Map<String, Object> map) {
        this.map = map;
    }

    @Override
    public Object get(String key) {
        return map.get(key);
    }

    public Map<String, Object> getMap() {
        return map;
    }
}
