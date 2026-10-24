package com.biojaveriana.api.specimen.exceptions;

import org.springframework.http.HttpStatus;
import com.biojaveriana.api.common.exceptions.ServiceException;

public class SpeciesAlreadyExistsException extends ServiceException {
    public SpeciesAlreadyExistsException(String scientificName, String commonName) {
        super(HttpStatus.CONFLICT, "SPECIES_ALREADY_EXISTS", buildMessage(scientificName, commonName));
    }

    private static String buildMessage(String scientificName, String commonName) {
        if (scientificName != null && commonName != null) {
            return String.format("Species already exists with scientific name '%s' or common name '%s'", scientificName, commonName);
        }

        if (scientificName != null) {
            return String.format("Species already exists with scientific name '%s'", scientificName);
        }

        if (commonName != null) {
            return String.format("Species already exists with common name '%s'", commonName);
        }

        return "Species already exists";
    }
}
