package com.biojaveriana.api.poi.exceptions;

import com.biojaveriana.api.common.exceptions.ServiceException;
import org.springframework.http.HttpStatus;

public class PoiTelemetryException extends ServiceException {
    public PoiTelemetryException(String message) {
        super(HttpStatus.BAD_REQUEST, "POI_TELEMETRY_ERROR", message);
    }
}
