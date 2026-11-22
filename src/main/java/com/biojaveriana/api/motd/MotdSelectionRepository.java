package com.biojaveriana.api.motd;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MotdSelectionRepository extends JpaRepository<MotdSelection, UUID> {
    Optional<MotdSelection> findByDisplayDate(LocalDate displayDate);

    void deleteByFunFactId(UUID funFactId);
}
