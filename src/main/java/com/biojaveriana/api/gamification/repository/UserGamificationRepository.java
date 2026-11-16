package com.biojaveriana.api.gamification.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.biojaveriana.api.gamification.model.UserGamification;

public interface UserGamificationRepository extends JpaRepository<UserGamification, UUID> {
}
