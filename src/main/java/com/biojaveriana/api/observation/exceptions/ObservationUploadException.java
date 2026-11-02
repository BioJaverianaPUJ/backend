package com.biojaveriana.api.observation.exceptions;

import org.springframework.http.HttpStatus;
import com.biojaveriana.api.common.exceptions.ServiceException;

public class ObservationUploadException extends ServiceException {
    public ObservationUploadException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "OBSERVATION_UPLOAD_FAILED", message);
        initCause(cause);
    }
}
