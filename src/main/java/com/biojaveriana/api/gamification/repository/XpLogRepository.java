package com.biojaveriana.api.gamification.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.biojaveriana.api.gamification.model.XpLog;

public interface XpLogRepository extends JpaRepository<XpLog, UUID> {
    Page<XpLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
