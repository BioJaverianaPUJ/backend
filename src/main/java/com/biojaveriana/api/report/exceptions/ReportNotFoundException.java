package com.biojaveriana.api.report.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class ReportNotFoundException extends ServiceException {
    public ReportNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "Report not found with id: " + id);
    }
}
