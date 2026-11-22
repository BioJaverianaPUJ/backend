package com.biojaveriana.api.observation;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.biojaveriana.api.user.User;

public interface ObservationRepository extends JpaRepository<Observation, UUID> {
    Page<Observation> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<Observation> findByHiddenFalseOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<Observation> findByUserAndHiddenFalseOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<Observation> findByHiddenFalseOrderByCreatedAtDesc(Pageable pageable);
    Page<Observation> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Observation> findByRevisionRequestedTrueOrderByCreatedAtDesc(Pageable pageable);

    long countByUserId(UUID userId);

    @Query("SELECT COUNT(DISTINCT o.speciesCommonName) FROM Observation o WHERE o.user.id = :userId AND o.speciesCommonName IS NOT NULL")
    long countDistinctSpeciesByUserId(@Param("userId") UUID userId);

    @Query("SELECT o.id FROM Observation o WHERE o.user.id = :userId")
    List<UUID> findIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT o.referencePictureKey FROM Observation o WHERE o.user.id = :userId")
    List<String> findReferencePictureKeysByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Observation o WHERE o.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}