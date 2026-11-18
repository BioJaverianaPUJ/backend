package com.biojaveriana.api.gamification.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.biojaveriana.api.gamification.model.UserGamification;
import jakarta.persistence.LockModeType;

public interface UserGamificationRepository extends JpaRepository<UserGamification, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ug FROM UserGamification ug WHERE ug.userId = :userId")
    Optional<UserGamification> findByIdWithPessimisticLock(@Param("userId") UUID userId);
}
