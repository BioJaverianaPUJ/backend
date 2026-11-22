package com.biojaveriana.api.motd;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FunFactRepository extends JpaRepository<FunFact, UUID> {
    Page<FunFact> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<FunFact> findByHiddenFalse();
}
