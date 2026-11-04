package com.biojaveriana.api.observation.dto;

import java.util.UUID;

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
public class ObservationMessage {
    private UUID observationId;
    private String s3Key;
    private double latitude;
    private double longitude;
    private int retryCount;
    private int maxRetries;
}
