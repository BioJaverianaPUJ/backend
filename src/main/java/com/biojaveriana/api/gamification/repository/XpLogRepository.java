package com.biojaveriana.api.gamification.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.biojaveriana.api.gamification.model.XpLog;

public interface XpLogRepository extends JpaRepository<XpLog, UUID> {
    Page<XpLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM XpLog x WHERE x.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
