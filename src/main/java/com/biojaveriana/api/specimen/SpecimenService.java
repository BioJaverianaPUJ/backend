package com.biojaveriana.api.specimen;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.biojaveriana.api.specimen.dto.SpecimenCreateDTO;
import com.biojaveriana.api.specimen.dto.SpecimenUpdateDTO;
import com.biojaveriana.api.specimen.exceptions.SpecimenNotFoundException;
import com.biojaveriana.api.specimen.exceptions.SpeciesNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpecimenService {
    private final SpecimenRepository specimenRepository;
    private final SpeciesRepository speciesRepository;

    public Page<Specimen> findAll(Pageable pageable) {
        return specimenRepository.findAll(pageable);
    }

    public Specimen findById(UUID id) {
        return specimenRepository.findById(id)
            .orElseThrow(() -> new SpecimenNotFoundException(id));
    }

    public Specimen create(SpecimenCreateDTO dto) {
        var species = speciesRepository.findById(dto.speciesId())
            .orElseThrow(() -> new SpeciesNotFoundException(dto.speciesId()));

        var specimen = dto.toEntity(species);
        return specimenRepository.save(specimen);
    }

    public Specimen update(UUID id, SpecimenUpdateDTO dto) {
        var specimen = specimenRepository.findById(id)
                .orElseThrow(() -> new SpecimenNotFoundException(id));

        if (dto.latitude() != null) specimen.setLatitude(dto.latitude());
        if (dto.longitude() != null) specimen.setLongitude(dto.longitude());
        if (dto.circumference() != null) specimen.setCircumference(dto.circumference());
        if (dto.height() != null) specimen.setHeight(dto.height());
        if (dto.healthStatus() != null) specimen.setHealthStatus(dto.healthStatus());
        if (dto.notes() != null) specimen.setNotes(dto.notes());
        if (dto.observationDate() != null) specimen.setObservationDate(dto.observationDate());

        return specimenRepository.save(specimen);
    }

    public void delete(UUID id) {
        if (!specimenRepository.existsById(id)) {
            throw new SpecimenNotFoundException(id);
        }

        specimenRepository.deleteById(id);
    }
}
