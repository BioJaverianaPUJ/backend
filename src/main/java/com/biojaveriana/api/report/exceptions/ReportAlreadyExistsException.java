package com.biojaveriana.api.report.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class ReportAlreadyExistsException extends ServiceException {
    public ReportAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "REPORT_ALREADY_EXISTS", "You have already reported this content");
    }
}
