package com.biojaveriana.api.specimen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.biojaveriana.api.specimen.dto.SpecimenCreateRequest;
import com.biojaveriana.api.specimen.dto.SpecimenUpdateRequest;
import com.biojaveriana.api.specimen.exceptions.SpecimenNotFoundException;
import com.biojaveriana.api.specimen.exceptions.SpeciesNotFoundException;

@ExtendWith(MockitoExtension.class)
class SpecimenServiceTest {

    @Mock
    private SpecimenRepository specimenRepository;

    @Mock
    private SpeciesRepository speciesRepository;

    private SpecimenService specimenService;
    private Species species;
    private UUID speciesId;
    private UUID specimenId;

    @BeforeEach
    void setUp() {
        specimenService = new SpecimenService(specimenRepository, speciesRepository);
        speciesId = UUID.randomUUID();
        specimenId = UUID.randomUUID();
        species = Species.builder().id(speciesId).scientificName("Ficus benjamina").commonName("Weeping fig").family("Moraceae").build();
    }

    @Nested
    @DisplayName("read")
    class ReadTests {

        @Test
        @DisplayName("findAll delegates to the repository")
        void findAllDelegates() {
            Pageable pageable = Pageable.ofSize(20);
            Page<Specimen> page = new PageImpl<>(List.of());
            when(specimenRepository.findAll(pageable)).thenReturn(page);

            assertSame(page, specimenService.findAll(pageable));
        }

        @Test
        @DisplayName("findById returns the stored specimen")
        void findByIdReturnsSpecimen() {
            Specimen specimen = Specimen.builder().id(specimenId).species(species).latitude(1.0).longitude(2.0).build();
            when(specimenRepository.findById(specimenId)).thenReturn(Optional.of(specimen));

            assertSame(specimen, specimenService.findById(specimenId));
        }

        @Test
        @DisplayName("findById on a missing id throws not found")
        void findByIdMissingThrows() {
            when(specimenRepository.findById(specimenId)).thenReturn(Optional.empty());

            assertThrows(SpecimenNotFoundException.class, () -> specimenService.findById(specimenId));
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("creates a specimen linked to the resolved species")
        void createLinksResolvedSpecies() {
            when(speciesRepository.findById(speciesId)).thenReturn(Optional.of(species));
            when(specimenRepository.save(any(Specimen.class))).thenAnswer(inv -> inv.getArgument(0));

            Specimen specimen = specimenService.create(
                new SpecimenCreateRequest(speciesId, 1.0, 2.0, 3.0, 4.0, HealthStatus.WELL, "healthy", null));

            assertSame(species, specimen.getSpecies());
            assertEquals(1.0, specimen.getLatitude());
            assertEquals(HealthStatus.WELL, specimen.getHealthStatus());
            verify(specimenRepository).save(specimen);
        }

        @Test
        @DisplayName("creating with an unknown species throws and saves nothing")
        void createWithUnknownSpeciesThrows() {
            UUID unknownSpeciesId = UUID.randomUUID();
            when(speciesRepository.findById(unknownSpeciesId)).thenReturn(Optional.empty());

            assertThrows(SpeciesNotFoundException.class, () -> specimenService.create(
                new SpecimenCreateRequest(unknownSpeciesId, 1.0, 2.0, null, null, null, "notes", null)));

            verify(specimenRepository, never()).save(any(Specimen.class));
        }
    }


    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("only the provided fields are replaced")
        void partialUpdateReplacesOnlyProvidedFields() {
            Specimen specimen = Specimen.builder()
                .id(specimenId).species(species)
                .latitude(1.0).longitude(2.0)
                .circumference(3.0).height(4.0)
                .healthStatus(HealthStatus.WELL).notes("old")
                .build();
            when(specimenRepository.findById(specimenId)).thenReturn(Optional.of(specimen));
            when(specimenRepository.save(any(Specimen.class))).thenAnswer(inv -> inv.getArgument(0));

            Specimen updated = specimenService.update(specimenId,
                new SpecimenUpdateRequest(null, 9.9, null, null, HealthStatus.POOR, "new notes", null));

            assertEquals(1.0, updated.getLatitude());
            assertEquals(9.9, updated.getLongitude());
            assertEquals(3.0, updated.getCircumference());
            assertEquals(4.0, updated.getHeight());
            assertEquals(HealthStatus.POOR, updated.getHealthStatus());
            assertEquals("new notes", updated.getNotes());
        }

        @Test
        @DisplayName("updating a missing specimen throws not found")
        void updateMissingThrows() {
            when(specimenRepository.findById(specimenId)).thenReturn(Optional.empty());

            assertThrows(SpecimenNotFoundException.class, () -> specimenService.update(specimenId,
                new SpecimenUpdateRequest(1.0, 2.0, null, null, null, null, null)));

            verify(specimenRepository, never()).save(any(Specimen.class));
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("deletes an existing specimen")
        void deleteExisting() {
            when(specimenRepository.existsById(specimenId)).thenReturn(true);

            specimenService.delete(specimenId);

            verify(specimenRepository).deleteById(specimenId);
        }

        @Test
        @DisplayName("deleting a missing specimen throws and deletes nothing")
        void deleteMissingThrows() {
            when(specimenRepository.existsById(specimenId)).thenReturn(false);

            assertThrows(SpecimenNotFoundException.class, () -> specimenService.delete(specimenId));

            verify(specimenRepository, never()).deleteById(any(UUID.class));
        }
    }
}
