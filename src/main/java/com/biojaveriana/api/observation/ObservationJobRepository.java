package com.biojaveriana.api.observation;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObservationJobRepository extends JpaRepository<ObservationJob, UUID> {
    Optional<ObservationJob> findByObservationId(UUID observationId);

    List<ObservationJob> findByStatusAndNextRetryAtLessThanEqual(JobStatus status, Instant nextRetryAt);

    @Modifying
    @Query("DELETE FROM ObservationJob oj WHERE oj.observation.id IN :observationIds")
    void deleteByObservationIdIn(@Param("observationIds") Collection<UUID> observationIds);
}
