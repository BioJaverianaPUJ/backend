package com.biojaveriana.api.motd;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.biojaveriana.api.motd.dto.MotdResponse;
import com.biojaveriana.api.s3.S3UrlGenerator;
import com.biojaveriana.api.user.Gender;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

@ExtendWith(MockitoExtension.class)
class MotdServiceTest {

    @Mock
    private MotdSelectionRepository motdSelectionRepository;

    @Mock
    private FunFactRepository funFactRepository;

    @Mock
    private S3UrlGenerator s3UrlGenerator;

    private MotdService motdService;

    private User admin;

    @BeforeEach
    void setUp() {
        motdService = new MotdService(motdSelectionRepository, funFactRepository, s3UrlGenerator);

        admin = User.builder()
            .id(UUID.randomUUID())
            .username("admin")
            .email("admin@example.com")
            .passwordHash("hash")
            .firstName("Admin")
            .lastName("User")
            .gender(Gender.OTHER)
            .role(Role.ADMIN)
            .build();
    }

    @Test
    @DisplayName("today: when no selection exists, one is created from the available fun facts")
    void todayShouldCreateSelectionWhenMissing() {
        LocalDate today = LocalDate.now();
        FunFact factA = buildFunFact(false);
        FunFact factB = buildFunFact(false);
        AtomicReference<MotdSelection> saved = new AtomicReference<>();
        when(motdSelectionRepository.findByDisplayDate(today)).thenAnswer(inv ->
            saved.get() != null ? Optional.of(saved.get()) : Optional.empty());
        when(motdSelectionRepository.save(any(MotdSelection.class))).thenAnswer(inv -> {
            MotdSelection selection = inv.getArgument(0);
            selection.setId(UUID.randomUUID());
            saved.set(selection);
            return selection;
        });
        when(funFactRepository.findByHiddenFalse()).thenReturn(List.of(factA, factB));

        Optional<MotdResponse> response = motdService.today();

        assertTrue(response.isPresent());
        assertEquals(today, response.get().displayDate());
        Set<UUID> candidates = Set.of(factA.getId(), factB.getId());
        assertTrue(candidates.contains(response.get().funFact().id()),
            "the selected fun fact must be one of the available fun facts");
        verify(motdSelectionRepository).save(any(MotdSelection.class));
    }

    @Test
    @DisplayName("today: an existing selection for today is reused without creating a new one")
    void todayShouldReuseExistingSelection() {
        LocalDate today = LocalDate.now();
        FunFact fact = buildFunFact(false);
        MotdSelection existing = MotdSelection.builder()
            .id(UUID.randomUUID())
            .funFact(fact)
            .displayDate(today)
            .selectedAt(Instant.now())
            .build();
        when(motdSelectionRepository.findByDisplayDate(today)).thenReturn(Optional.of(existing));

        Optional<MotdResponse> response = motdService.today();

        assertTrue(response.isPresent());
        assertEquals(fact.getId(), response.get().funFact().id());
        verify(motdSelectionRepository, never()).save(any(MotdSelection.class));
        verify(funFactRepository, never()).findByHiddenFalse();
    }

    @Test
    @DisplayName("ensureSelection: only non-hidden fun facts are candidates")
    void ensureSelectionShouldOnlyPickVisibleFunFacts() {
        LocalDate today = LocalDate.now();
        FunFact visible = buildFunFact(false);
        when(motdSelectionRepository.findByDisplayDate(today)).thenReturn(Optional.empty());
        when(funFactRepository.findByHiddenFalse()).thenReturn(List.of(visible));

        motdService.ensureSelection(today);

        ArgumentCaptor<MotdSelection> captor = ArgumentCaptor.forClass(MotdSelection.class);
        verify(motdSelectionRepository).save(captor.capture());
        assertEquals(visible.getId(), captor.getValue().getFunFact().getId());
        assertEquals(today, captor.getValue().getDisplayDate());
    }

    @Test
    @DisplayName("ensureSelection: does nothing when a selection already exists for the date")
    void ensureSelectionShouldNotCreateSelectionWhenPresent() {
        LocalDate today = LocalDate.now();
        MotdSelection existing = MotdSelection.builder()
            .id(UUID.randomUUID())
            .funFact(buildFunFact(false))
            .displayDate(today)
            .selectedAt(Instant.now())
            .build();
        when(motdSelectionRepository.findByDisplayDate(today)).thenReturn(Optional.of(existing));

        motdService.ensureSelection(today);

        verify(motdSelectionRepository, never()).save(any(MotdSelection.class));
        verify(funFactRepository, never()).findByHiddenFalse();
    }

    @Test
    @DisplayName("today: returns empty when no fun facts are available")
    void todayShouldReturnEmptyWhenNoFunFactsAvailable() {
        LocalDate today = LocalDate.now();
        when(motdSelectionRepository.findByDisplayDate(today)).thenReturn(Optional.empty());
        when(funFactRepository.findByHiddenFalse()).thenReturn(List.of());

        Optional<MotdResponse> response = motdService.today();

        assertTrue(response.isEmpty());
        verify(motdSelectionRepository, never()).save(any(MotdSelection.class));
    }

    private FunFact buildFunFact(boolean hidden) {
        return FunFact.builder()
            .id(UUID.randomUUID())
            .author(admin)
            .title("Fun fact " + UUID.randomUUID())
            .description("A fun fact description")
            .hidden(hidden)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
