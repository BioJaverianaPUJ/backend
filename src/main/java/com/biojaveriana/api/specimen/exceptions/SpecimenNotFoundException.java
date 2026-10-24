package com.biojaveriana.api.specimen.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class SpecimenNotFoundException extends ServiceException {
    public SpecimenNotFoundException(UUID specimenId) {
        super(HttpStatus.NOT_FOUND, "SPECIMEN_NOT_FOUND", "Specimen not found with ID: " + specimenId);
    }
}