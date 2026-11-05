package com.biojaveriana.api.observation;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.biojaveriana.api.user.User;

public interface ObservationRepository extends JpaRepository<Observation, UUID> {
    Page<Observation> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<Observation> findByHiddenFalseOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<Observation> findByUserAndHiddenFalseOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<Observation> findByHiddenFalseOrderByCreatedAtDesc(Pageable pageable);
    Page<Observation> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Observation> findByRevisionRequestedTrueOrderByCreatedAtDesc(Pageable pageable);
}
