package com.biojaveriana.api.specimen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.biojaveriana.api.specimen.dto.SpecimenCreateDTO;
import com.biojaveriana.api.specimen.dto.SpecimenListResponse;
import com.biojaveriana.api.specimen.dto.SpecimenResponse;
import com.biojaveriana.api.specimen.dto.SpecimenUpdateDTO;

@ExtendWith(MockitoExtension.class)
class SpecimenControllerTest {

    @Mock
    private SpecimenService specimenService;

    private SpecimenController controller;

    @BeforeEach
    void setUp() {
        controller = new SpecimenController(specimenService);
    }

    @Test
    void findAll_usesIdSortAndMapsPage() {
        Specimen value = specimen();
        when(specimenService.findAll(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(value), PageRequest.of(2, 15), 31));

        SpecimenListResponse response = controller.findAll(2, 15);

        verify(specimenService).findAll(PageRequest.of(2, 15, Sort.by("id").ascending()));
        assertThat(response.content()).singleElement().extracting("id").isEqualTo(value.getId());
        assertThat(response.pagination()).isNotNull();
    }

    @Test
    void getSpecimenById_parsesIdAndMapsResponse() {
        Specimen value = specimen();
        when(specimenService.findById(value.getId())).thenReturn(value);

        SpecimenResponse response = controller.getSpecimenById(value.getId().toString());

        verify(specimenService).findById(value.getId());
        assertThat(response.id()).isEqualTo(value.getId());
        assertThat(response.speciesId()).isEqualTo(value.getSpecies().getId());
        assertThat(response.latitude()).isEqualTo(value.getLatitude());
    }

    @Test
    void create_delegatesAndMapsResponse() {
        Species species = species();
        SpecimenCreateDTO dto = new SpecimenCreateDTO(
            species.getId(), 4.5, -74.1, 1.2, 12.0, HealthStatus.WELL, "Observation", null
        );
        Specimen value = specimen();
        when(specimenService.create(dto)).thenReturn(value);

        SpecimenResponse response = controller.create(dto);

        verify(specimenService).create(dto);
        assertThat(response.id()).isEqualTo(value.getId());
    }

    @Test
    void update_parsesIdDelegatesAndMapsResponse() {
        Specimen value = specimen();
        SpecimenUpdateDTO dto = new SpecimenUpdateDTO(5.0, null, null, null, null, null, null);
        when(specimenService.update(value.getId(), dto)).thenReturn(value);

        SpecimenResponse response = controller.update(value.getId().toString(), dto);

        verify(specimenService).update(value.getId(), dto);
        assertThat(response.id()).isEqualTo(value.getId());
    }

    @Test
    void delete_parsesIdAndDelegates() {
        UUID id = UUID.randomUUID();

        controller.delete(id.toString());

        verify(specimenService).delete(id);
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
