package com.biojaveriana.api.motd;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FunFactImageRepository extends JpaRepository<FunFactImage, UUID> {
    void deleteByFunFactId(UUID funFactId);
}
