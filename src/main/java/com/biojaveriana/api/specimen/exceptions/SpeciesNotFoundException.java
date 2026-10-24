package com.biojaveriana.api.specimen.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.biojaveriana.api.common.exceptions.ServiceException;

public class SpeciesNotFoundException extends ServiceException {
    public SpeciesNotFoundException(UUID speciesId) {
        super(HttpStatus.NOT_FOUND, "SPECIES_NOT_FOUND", "Species not found with ID: " + speciesId);
    }
}
