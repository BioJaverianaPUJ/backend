package com.biojaveriana.api.job;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    @Query("SELECT j FROM Job j WHERE j.status = ?1 AND j.retries < j.maxRetries AND j.updatedAt < ?2")
    List<Job> findFailedJobsNeedingRetry(Instant deadline);

    Optional<Job> findByIdAndObservationId(UUID id, UUID observationId);

    List<Job> findByObservationIdOrderByCreatedAtDesc(UUID observationId);

    // --- Polymorphic ownership (Approach 1) ---
    Optional<Job> findByResourceTypeAndResourceId(String resourceType, UUID resourceId);
}
