package com.biojaveriana.api.motd;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.motd.dto.FunFactResponse;
import com.biojaveriana.api.motd.dto.MotdResponse;
import com.biojaveriana.api.s3.S3UrlGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MotdService {
    private final MotdSelectionRepository motdSelectionRepository;
    private final FunFactRepository funFactRepository;
    private final S3UrlGenerator s3UrlGenerator;

    /**
     * Guarantees that a selection exists for the given date. If none exists, a random
     * non-hidden fun fact is picked and persisted as the selection for that date.
     */
    @Transactional
    public void ensureSelection(LocalDate date) {
        if (motdSelectionRepository.findByDisplayDate(date).isPresent()) {
            return;
        }

        List<FunFact> candidates = funFactRepository.findByHiddenFalse();
        if (candidates.isEmpty()) {
            log.warn("No available fun facts to select the MOTD for date {}", date);
            return;
        }

        FunFact selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        var selection = MotdSelection.builder()
            .funFact(selected)
            .displayDate(date)
            .build();

        try {
            motdSelectionRepository.save(selection);
        } catch (DataIntegrityViolationException e) {
            // A concurrent request already created the selection for this date
            log.info("MOTD selection for date {} was already created concurrently", date);
            return;
        }

        log.info("MOTD selected for date {}: fun fact id={}", date, selected.getId());
    }

    @Transactional
    public Optional<MotdResponse> today() {
        LocalDate date = LocalDate.now();
        ensureSelection(date);
        return motdSelectionRepository.findByDisplayDate(date)
            .map(selection -> new MotdResponse(
                selection.getDisplayDate(),
                selection.getSelectedAt(),
                FunFactResponse.from(selection.getFunFact(), s3UrlGenerator)
            ));
    }

    @Transactional(readOnly = true)
    public Optional<MotdResponse> getByDisplayDate(LocalDate date) {
        return motdSelectionRepository.findByDisplayDate(date)
            .map(selection -> new MotdResponse(
                selection.getDisplayDate(),
                selection.getSelectedAt(),
                FunFactResponse.from(selection.getFunFact(), s3UrlGenerator)
            ));
    }
}
