package com.biojaveriana.api.specimen;

import java.util.UUID;

import jakarta.validation.Valid;
import com.biojaveriana.api.specimen.dto.SpeciesCreateDTO;
import com.biojaveriana.api.specimen.dto.SpeciesUpdateDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.biojaveriana.api.specimen.exceptions.SpeciesNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpeciesService {
    public final SpeciesRepository speciesRepository;

    public Page<Species> findAll(Pageable pageable) {
        return speciesRepository.findAll(pageable);
    }

    public Species findById(UUID id) {
        return speciesRepository.findById(id)
            .orElseThrow(() -> new SpeciesNotFoundException(id));
    }

    public Species create(@Valid SpeciesCreateDTO dto) {
        return speciesRepository.save(dto.toEntity());
    }

    public Species update(UUID id, @Valid SpeciesUpdateDTO dto) {
        var species = speciesRepository.findById(id)
                .orElseThrow(() -> new SpeciesNotFoundException(id));

        if (dto.scientificName() != null) {
            species.setScientificName(dto.scientificName());
        }
        if (dto.commonName() != null) {
            species.setCommonName(dto.commonName());
        }
        if (dto.family() != null) {
            species.setFamily(dto.family());
        }
        if (dto.woodDensity() != null) {
            species.setWoodDensity(dto.woodDensity());
        }
        return speciesRepository.save(species);
    }

    public void delete(UUID id) {
        if (!speciesRepository.existsById(id)) {
            throw new SpeciesNotFoundException(id);
        }

        speciesRepository.deleteById(id);
    }
}
