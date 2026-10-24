package com.biojaveriana.api.specimen;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.specimen.dto.SpeciesListResponse;
import com.biojaveriana.api.specimen.dto.SpeciesResponse;
import com.biojaveriana.api.specimen.dto.SpeciesCreateDTO;
import com.biojaveriana.api.specimen.dto.SpeciesUpdateDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/species")
@Validated
@RequiredArgsConstructor
public class SpeciesController {
    private final SpeciesService speciesService;

    @GetMapping
    public SpeciesListResponse findAll(
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @RequestParam(defaultValue = "20") @Max(value = 100) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("scientificName").ascending());
        var speciesPage = speciesService.findAll(pageable);
        return SpeciesListResponse.from(speciesPage);
    }

    @GetMapping("/{id}")
    public SpeciesResponse getSpeciesById(@PathVariable String id) {
        var species = speciesService.findById(UUID.fromString(id));
        return SpeciesResponse.from(species);
    }

    @PostMapping
    public SpeciesResponse create(@Valid @RequestBody SpeciesCreateDTO speciesRequest) {
        var species = speciesService.create(speciesRequest);
        return SpeciesResponse.from(species);
    }

    @PatchMapping("/{id}")
    public SpeciesResponse update(
            @PathVariable String id,
            @Valid @RequestBody SpeciesUpdateDTO speciesUpdate
    ) {
        var updatedSpecies = speciesService.update(UUID.fromString(id), speciesUpdate);
        return SpeciesResponse.from(updatedSpecies);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        speciesService.delete(UUID.fromString(id));
    }
}
