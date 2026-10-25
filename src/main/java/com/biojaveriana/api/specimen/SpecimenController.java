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

import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.specimen.dto.SpecimenCreateDTO;
import com.biojaveriana.api.specimen.dto.SpecimenListResponse;
import com.biojaveriana.api.specimen.dto.SpecimenResponse;
import com.biojaveriana.api.specimen.dto.SpecimenUpdateDTO;
import com.biojaveriana.api.user.Role;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/specimens")
@Validated
@RequiredArgsConstructor
public class SpecimenController {
    private final SpecimenService specimenService;

    @GetMapping
    public SpecimenListResponse findAll(
        @RequestParam(defaultValue = "0") @Min(value = 0) int page,
        @RequestParam(defaultValue = "20") @Max(value = 100) int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        var specimenPage = specimenService.findAll(pageable);
        return SpecimenListResponse.from(specimenPage);
    }

    @GetMapping("/{id}")
    public SpecimenResponse getSpecimenById(@PathVariable String id) {
        var specimen = specimenService.findById(UUID.fromString(id));
        return SpecimenResponse.from(specimen);
    }

    @PostMapping
    @HasRole(value = Role.ADMIN)
    public SpecimenResponse create(@Valid @RequestBody SpecimenCreateDTO specimenRequest) {
        var specimen = specimenService.create(specimenRequest);
        return SpecimenResponse.from(specimen);
    }

    @PatchMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public SpecimenResponse update(
        @PathVariable String id,
        @Valid @RequestBody SpecimenUpdateDTO specimenUpdate
    ) {
        var updatedSpecimen = specimenService.update(UUID.fromString(id), specimenUpdate);
        return SpecimenResponse.from(updatedSpecimen);
    }

    @DeleteMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public void delete(@PathVariable String id) {
        specimenService.delete(UUID.fromString(id));
    }
}
