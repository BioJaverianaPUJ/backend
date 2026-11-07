package com.biojaveriana.api.report.exceptions;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class ReportForbiddenException extends ServiceException {
    public ReportForbiddenException() {
        super(HttpStatus.FORBIDDEN, "REPORT_FORBIDDEN", "You do not have permission to perform this action");
    }
}
