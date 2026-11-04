package com.biojaveriana.api.observation;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ObservationRepository extends JpaRepository<Observation, UUID> {
    Optional<Observation> findByIdAndUserId(UUID id, UUID userId);

    // Admin: paginate all observations (sorted by newest first)
    Page<Observation> findAllBy(Pageable pageable);

    // Admin: filter by revisionRequested = true
    Page<Observation> findByRevisionRequestedTrue(Pageable pageable);

    // Admin: filter by both revisionRequested and checked status
    Page<Observation> findByRevisionRequestedTrueAndCheckedTrue(Pageable pageable);

    // Admin: filter by unreviewed revisions (revisionRequested=true AND checked=false)
    Page<Observation> findByRevisionRequestedTrueAndCheckedFalse(Pageable pageable);
}
