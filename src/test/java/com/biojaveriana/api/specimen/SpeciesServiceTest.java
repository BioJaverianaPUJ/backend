package com.biojaveriana.api.specimen;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.biojaveriana.api.observation.ObservationRepository;
import com.biojaveriana.api.specimen.exceptions.SpeciesNotFoundException;

@ExtendWith(MockitoExtension.class)
class SpeciesServiceTest {

    @Mock
    private SpeciesRepository speciesRepository;

    @Mock
    private SpecimenRepository specimenRepository;

    @Mock
    private ObservationRepository observationRepository;

    private SpeciesService speciesService;

    private UUID speciesId;

    @BeforeEach
    void setUp() {
        speciesService = new SpeciesService(speciesRepository, specimenRepository, observationRepository);
        speciesId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("rejects deletion of an unknown species")
        void shouldRejectUnknownSpecies() {
            when(speciesRepository.existsById(speciesId)).thenReturn(false);

            assertThrows(SpeciesNotFoundException.class, () -> speciesService.delete(speciesId));

            verify(observationRepository, never()).clearSpecimenBySpeciesId(any());
            verify(specimenRepository, never()).deleteBySpeciesId(any());
            verify(speciesRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("cascades deletion to specimens in a foreign-key-safe order")
        void shouldCascadeDeletionToSpecimens() {
            when(speciesRepository.existsById(speciesId)).thenReturn(true);

            speciesService.delete(speciesId);

            InOrder inOrder = inOrder(observationRepository, specimenRepository, speciesRepository);
            inOrder.verify(observationRepository).clearSpecimenBySpeciesId(speciesId);
            inOrder.verify(specimenRepository).deleteBySpeciesId(speciesId);
            inOrder.verify(speciesRepository).deleteById(speciesId);
        }
    }
}
