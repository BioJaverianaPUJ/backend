package com.biojaveriana.api.specimen;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeciesRepository extends JpaRepository<Species, UUID> {}
