package com.biojaveriana.api.poi.exceptions;

import com.biojaveriana.api.common.exceptions.NotFoundException;

public class PoiNotFoundException extends NotFoundException {
    public PoiNotFoundException() {
        super("The requested POI was not found");
    }

    public PoiNotFoundException(String message) {
        super(message);
    }

    public PoiNotFoundException(java.util.UUID id) {
        super("POI with id " + id + " was not found");
    }
}
