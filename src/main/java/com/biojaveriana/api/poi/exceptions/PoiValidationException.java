package com.biojaveriana.api.poi.exceptions;

import com.biojaveriana.api.common.exceptions.ValidationException;

public class PoiValidationException extends ValidationException {
    public PoiValidationException(String message) {
        super(message);
    }
}
