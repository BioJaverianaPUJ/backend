package com.biojaveriana.api.specimen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.biojaveriana.api.specimen.dto.SpeciesCreateRequest;
import com.biojaveriana.api.specimen.dto.SpeciesUpdateRequest;
import com.biojaveriana.api.specimen.exceptions.SpeciesAlreadyExistsException;
import com.biojaveriana.api.specimen.exceptions.SpeciesNotFoundException;

@ExtendWith(MockitoExtension.class)
class SpeciesServiceTest {

    @Mock
    private SpeciesRepository speciesRepository;

    private SpeciesService speciesService;

    @BeforeEach
    void setUp() {
        speciesService = new SpeciesService(speciesRepository);
    }

    @Test
    void findAll_delegatesToRepository() {
        Pageable pageable = PageRequest.of(1, 10);
        PageImpl<Species> page = new PageImpl<>(List.of(species()));
        when(speciesRepository.findAll(pageable)).thenReturn(page);

        assertThat(speciesService.findAll(pageable)).isSameAs(page);
        verify(speciesRepository).findAll(pageable);
    }

    @Test
    void findById_returnsSpecies() {
        Species value = species();
        when(speciesRepository.findById(value.getId())).thenReturn(Optional.of(value));

        assertThat(speciesService.findById(value.getId())).isSameAs(value);
    }

    @Test
    void findById_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(speciesRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> speciesService.findById(id))
            .isInstanceOf(SpeciesNotFoundException.class);
    }

    @Test
    void create_savesEntityWhenNamesAreAvailable() {
        SpeciesCreateRequest dto = new SpeciesCreateRequest("Quercus humboldtii", "Roble", "Fagaceae", 0.62);
        Species saved = species();
        when(speciesRepository.existsByScientificName(dto.scientificName())).thenReturn(false);
        when(speciesRepository.existsByCommonName(dto.commonName())).thenReturn(false);
        when(speciesRepository.save(any(Species.class))).thenReturn(saved);

        assertThat(speciesService.create(dto)).isSameAs(saved);
        verify(speciesRepository).save(any(Species.class));
    }

    @Test
    void create_rejectsExistingScientificName() {
        SpeciesCreateRequest dto = new SpeciesCreateRequest("Quercus humboldtii", "Roble", "Fagaceae", 0.62);
        when(speciesRepository.existsByScientificName(dto.scientificName())).thenReturn(true);

        assertThatThrownBy(() -> speciesService.create(dto))
            .isInstanceOf(SpeciesAlreadyExistsException.class);
        verify(speciesRepository, never()).existsByCommonName(any());
        verify(speciesRepository, never()).save(any());
    }

    @Test
    void create_rejectsExistingCommonName() {
        SpeciesCreateRequest dto = new SpeciesCreateRequest("Quercus humboldtii", "Roble", "Fagaceae", 0.62);
        when(speciesRepository.existsByScientificName(dto.scientificName())).thenReturn(false);
        when(speciesRepository.existsByCommonName(dto.commonName())).thenReturn(true);

        assertThatThrownBy(() -> speciesService.create(dto))
            .isInstanceOf(SpeciesAlreadyExistsException.class);
        verify(speciesRepository, never()).save(any());
    }

    @Test
    void update_changesProvidedFieldsAndSaves() {
        Species value = species();
        when(speciesRepository.findById(value.getId())).thenReturn(Optional.of(value));
        when(speciesRepository.save(value)).thenReturn(value);
        SpeciesUpdateRequest dto = new SpeciesUpdateRequest("New scientific", "New common", "New family", 0.8);

        assertThat(speciesService.update(value.getId(), dto)).isSameAs(value);
        assertThat(value.getScientificName()).isEqualTo("New scientific");
        assertThat(value.getCommonName()).isEqualTo("New common");
        assertThat(value.getFamily()).isEqualTo("New family");
        assertThat(value.getWoodDensity()).isEqualTo(0.8);
        verify(speciesRepository).save(value);
    }

    @Test
    void update_preservesNullFields() {
        Species value = species();
        when(speciesRepository.findById(value.getId())).thenReturn(Optional.of(value));
        when(speciesRepository.save(value)).thenReturn(value);

        speciesService.update(value.getId(), new SpeciesUpdateRequest(null, null, null, null));

        assertThat(value.getScientificName()).isEqualTo("Quercus humboldtii");
        assertThat(value.getCommonName()).isEqualTo("Roble");
        assertThat(value.getFamily()).isEqualTo("Fagaceae");
        assertThat(value.getWoodDensity()).isEqualTo(0.62);
    }

    @Test
    void update_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(speciesRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> speciesService.update(id, new SpeciesUpdateRequest(null, null, null, null)))
            .isInstanceOf(SpeciesNotFoundException.class);
    }

    @Test
    void delete_deletesExistingSpecies() {
        UUID id = UUID.randomUUID();
        when(speciesRepository.existsById(id)).thenReturn(true);

        speciesService.delete(id);

        verify(speciesRepository).deleteById(id);
    }

    @Test
    void delete_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(speciesRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> speciesService.delete(id))
            .isInstanceOf(SpeciesNotFoundException.class);
        verify(speciesRepository, never()).deleteById(any());
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
}
