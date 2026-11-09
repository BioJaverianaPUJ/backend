package com.biojaveriana.api.specimen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.biojaveriana.api.specimen.dto.SpeciesCreateRequest;
import com.biojaveriana.api.specimen.dto.SpeciesListResponse;
import com.biojaveriana.api.specimen.dto.SpeciesResponse;
import com.biojaveriana.api.specimen.dto.SpeciesUpdateRequest;
import com.biojaveriana.api.user.User;

@ExtendWith(MockitoExtension.class)
class SpeciesControllerTest {

    @Mock
    private SpeciesService speciesService;

    private SpeciesController controller;

    @BeforeEach
    void setUp() {
        controller = new SpeciesController(speciesService);
    }

    @Test
    void findAll_usesScientificNameSortAndMapsPage() {
        Species value = species();
        when(speciesService.findAll(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(value), PageRequest.of(1, 10), 11));

        SpeciesListResponse response = controller.findAll(1, 10).getBody();

        verify(speciesService).findAll(PageRequest.of(1, 10, Sort.by("scientificName").ascending()));
        assertThat(response.content()).singleElement().extracting("id").isEqualTo(value.getId());
        assertThat(response.pagination()).isNotNull();
    }

    @Test
    void getSpeciesById_parsesIdAndMapsResponse() {
        Species value = species();
        when(speciesService.findById(value.getId())).thenReturn(value);

        SpeciesResponse response = controller.getSpeciesById(value.getId()).getBody();

        verify(speciesService).findById(value.getId());
        assertThat(response.id()).isEqualTo(value.getId());
        assertThat(response.scientificName()).isEqualTo(value.getScientificName());
        assertThat(response.commonName()).isEqualTo(value.getCommonName());
    }

    @Test
    void create_delegatesAndMapsResponse() {
        SpeciesCreateRequest dto = new SpeciesCreateRequest("Quercus humboldtii", "Roble", "Fagaceae", 0.62);
        Species value = species();
        when(speciesService.create(dto)).thenReturn(value);

        SpeciesResponse response = controller.create(admin(), dto).getBody();

        verify(speciesService).create(dto);
        assertThat(response.id()).isEqualTo(value.getId());
    }

    @Test
    void update_parsesIdDelegatesAndMapsResponse() {
        Species value = species();
        SpeciesUpdateRequest dto = new SpeciesUpdateRequest("New", null, null, null);
        when(speciesService.update(value.getId(), dto)).thenReturn(value);

        SpeciesResponse response = controller.update(value.getId(), admin(), dto).getBody();

        verify(speciesService).update(value.getId(), dto);
        assertThat(response.id()).isEqualTo(value.getId());
    }

    @Test
    void delete_parsesIdAndDelegates() {
        UUID id = UUID.randomUUID();

        controller.delete(id, admin());

        verify(speciesService).delete(id);
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

    private static User admin() {
        return User.builder().id(UUID.randomUUID()).build();
    }
}
