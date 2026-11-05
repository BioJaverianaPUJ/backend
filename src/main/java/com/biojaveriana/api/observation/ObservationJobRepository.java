package com.biojaveriana.api.observation;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ObservationJobRepository extends JpaRepository<ObservationJob, UUID> {
    Optional<ObservationJob> findByObservationId(UUID observationId);
}
