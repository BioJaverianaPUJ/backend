package com.biojaveriana.api.specimen;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecimenRepository extends JpaRepository<Specimen, UUID> {

    /**
     * Find all specimens whose scientific name contains the given search string (case-insensitive).
     * Used for fuzzy matching of observed species names against the specimen database.
     */
    List<Specimen> findBySpeciesScientificNameContainingIgnoreCase(String scientificName);
}
