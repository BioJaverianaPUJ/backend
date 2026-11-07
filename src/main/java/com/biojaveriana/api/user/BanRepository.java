package com.biojaveriana.api.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BanRepository extends JpaRepository<Ban, UUID> {
    @Query("SELECT b FROM Ban b WHERE b.user.id = :userId AND b.status = 'ACTIVE'")
    Optional<Ban> findActiveBanByUserId(@Param("userId") UUID userId);

    @Query("SELECT b FROM Ban b WHERE b.user.id = :userId AND b.status = 'ACTIVE' AND b.expiresAt IS NOT NULL ORDER BY b.expiresAt ASC")
    List<Ban> findActiveTemporalBansByUserId(@Param("userId") UUID userId);

    List<Ban> findByUserOrderByCreatedAtDesc(User user);

    List<Ban> findByOrderByCreatedAtDesc();

    Page<Ban> findByOrderByCreatedAtDesc(Pageable pageable);

    Page<Ban> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}