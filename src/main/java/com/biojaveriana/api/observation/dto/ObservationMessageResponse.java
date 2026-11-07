package com.biojaveriana.api.observation.dto;

import java.util.UUID;

import com.biojaveriana.api.observation.ObservationType;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObservationMessageResponse {
    private UUID observationId;
    private boolean success;
    private String speciesCommonName;
    private Double certainty;
    private Double similarityScore;
    private ObservationType observationType;
    private String errorMessage;
}
