package com.biojaveriana.api.specimen;

import java.util.UUID;

import jakarta.validation.Valid;

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

    public Species create(@Valid Species species) {
        if (species.getId() != null) {
            return speciesRepository.save(species);
        }

        return speciesRepository.save(species);
    }

    public Species update(UUID id, @Valid Species speciesUpdate) {
        if (!speciesRepository.existsById(id)) {
            throw new SpeciesNotFoundException(id);
        }

        speciesUpdate.setId(id);

        return speciesRepository.save(speciesUpdate);
    }

    public void delete(UUID id) {
        if (!speciesRepository.existsById(id)) {
            throw new SpeciesNotFoundException(id);
        }

        speciesRepository.deleteById(id);
    }
}
