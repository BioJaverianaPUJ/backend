package com.biojaveriana.api.specimen;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpecimenRepository extends JpaRepository<Specimen, UUID> {

    /**
     * Find all specimens whose scientific name contains the given search string (case-insensitive).
     * Used for fuzzy matching of observed species names against the specimen database.
     */
    List<Specimen> findBySpeciesScientificNameContainingIgnoreCase(String scientificName);

    @Modifying
    @Query("DELETE FROM Specimen s WHERE s.species.id = :speciesId")
    void deleteBySpeciesId(@Param("speciesId") UUID speciesId);
}
