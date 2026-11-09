package com.biojaveriana.api.specimen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    @BeforeEach
    void setUp() {
        specimenService = new SpecimenService(specimenRepository, speciesRepository);
    }

    @Test
    void findAll_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        PageImpl<Specimen> page = new PageImpl<>(List.of(specimen()));
        when(specimenRepository.findAll(pageable)).thenReturn(page);

        assertThat(specimenService.findAll(pageable)).isSameAs(page);
        verify(specimenRepository).findAll(pageable);
    }

    @Test
    void findById_returnsSpecimen() {
        Specimen value = specimen();
        when(specimenRepository.findById(value.getId())).thenReturn(Optional.of(value));

        assertThat(specimenService.findById(value.getId())).isSameAs(value);
    }

    @Test
    void findById_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(specimenRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specimenService.findById(id))
            .isInstanceOf(SpecimenNotFoundException.class);
    }

    @Test
    void create_findsSpeciesAndSavesSpecimen() {
        Species species = species();
        SpecimenCreateRequest dto = new SpecimenCreateRequest(
            species.getId(), 4.5, -74.1, 1.2, 12.0, HealthStatus.WELL,
            "Healthy specimen", Instant.parse("2026-01-01T00:00:00Z")
        );
        Specimen saved = specimen();
        when(speciesRepository.findById(species.getId())).thenReturn(Optional.of(species));
        when(specimenRepository.save(any(Specimen.class))).thenReturn(saved);

        assertThat(specimenService.create(dto)).isSameAs(saved);
        verify(specimenRepository).save(any(Specimen.class));
    }

    @Test
    void create_throwsWhenSpeciesMissing() {
        UUID speciesId = UUID.randomUUID();
        SpecimenCreateRequest dto = new SpecimenCreateRequest(
            speciesId, 4.5, -74.1, null, null, null, "Observation", null
        );
        when(speciesRepository.findById(speciesId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specimenService.create(dto))
            .isInstanceOf(SpeciesNotFoundException.class);
        verify(specimenRepository, never()).save(any());
    }

    @Test
    void update_changesProvidedFieldsAndSaves() {
        Specimen value = specimen();
        when(specimenRepository.findById(value.getId())).thenReturn(Optional.of(value));
        when(specimenRepository.save(value)).thenReturn(value);
        Instant date = Instant.parse("2026-02-01T00:00:00Z");
        SpecimenUpdateRequest dto = new SpecimenUpdateRequest(
            5.0, -75.0, 2.0, 14.0, HealthStatus.FAIR, "Updated", date
        );

        assertThat(specimenService.update(value.getId(), dto)).isSameAs(value);
        assertThat(value.getLatitude()).isEqualTo(5.0);
        assertThat(value.getLongitude()).isEqualTo(-75.0);
        assertThat(value.getCircumference()).isEqualTo(2.0);
        assertThat(value.getHeight()).isEqualTo(14.0);
        assertThat(value.getHealthStatus()).isEqualTo(HealthStatus.FAIR);
        assertThat(value.getNotes()).isEqualTo("Updated");
        assertThat(value.getObservationDate()).isEqualTo(date);
    }

    @Test
    void update_preservesNullFields() {
        Specimen value = specimen();
        when(specimenRepository.findById(value.getId())).thenReturn(Optional.of(value));
        when(specimenRepository.save(value)).thenReturn(value);

        specimenService.update(value.getId(), new SpecimenUpdateRequest(
            null, null, null, null, null, null, null
        ));

        assertThat(value.getLatitude()).isEqualTo(4.5);
        assertThat(value.getLongitude()).isEqualTo(-74.1);
        assertThat(value.getNotes()).isEqualTo("Observation");
    }

    @Test
    void update_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(specimenRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specimenService.update(id, new SpecimenUpdateRequest(
            null, null, null, null, null, null, null
        ))).isInstanceOf(SpecimenNotFoundException.class);
    }

    @Test
    void delete_deletesExistingSpecimen() {
        UUID id = UUID.randomUUID();
        when(specimenRepository.existsById(id)).thenReturn(true);

        specimenService.delete(id);

        verify(specimenRepository).deleteById(id);
    }

    @Test
    void delete_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(specimenRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> specimenService.delete(id))
            .isInstanceOf(SpecimenNotFoundException.class);
        verify(specimenRepository, never()).deleteById(any());
    }

    private static Species species() {
        return Species.builder()
            .id(UUID.randomUUID())
            .scientificName("Quercus humboldtii")
            .commonName("Roble")
            .family("Fagaceae")
            .woodDensity(0.62)
            .build();
    }

    private static Specimen specimen() {
        return Specimen.builder()
            .id(UUID.randomUUID())
            .species(species())
            .latitude(4.5)
            .longitude(-74.1)
            .circumference(1.2)
            .height(12.0)
            .healthStatus(HealthStatus.WELL)
            .notes("Observation")
            .observationDate(Instant.parse("2026-01-01T00:00:00Z"))
            .build();
    }
}
