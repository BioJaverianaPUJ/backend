package com.biojaveriana.api.specimen;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.biojaveriana.api.specimen.exceptions.SpeciesNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpeciesService {
    public final SpeciesRepository speciesRepository;

    public Page<Species> findAll(Pageable pageble) {
        return speciesRepository.findAll(pageble);
    }

    public Species findById(UUID id) {
        return speciesRepository.findById(id)
            .orElseThrow(() -> new SpeciesNotFoundException(id));
    }

    
}
