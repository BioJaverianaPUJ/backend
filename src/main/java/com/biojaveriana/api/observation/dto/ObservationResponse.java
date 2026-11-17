package com.biojaveriana.api.observation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

import com.biojaveriana.api.observation.Observation;
import com.biojaveriana.api.observation.ObservationType;
import com.biojaveriana.api.s3.S3UrlGenerator;
import com.biojaveriana.api.specimen.dto.SpecimenResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Response body containing observation details", example = "{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"latitude\": 4.5708, \"longitude\": -75.7521, \"checked\": false, \"revisionRequested\": false, \"speciesCommonName\": \"English Oak\", \"certainty\": 0.95, \"similarityScore\": 0.92, \"observationType\": \"PLANT\", \"specimen\": null, \"imageUrl\": \"https://s3.example.com/uploads/observation/abc123.jpg\", \"createdAt\": \"2026-06-15T10:00:00Z\"}")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObservationResponse {
    @Schema(description = "Unique observation identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Latitude of the observation", example = "4.5708")
    private Double latitude;

    @Schema(description = "Longitude of the observation", example = "-75.7521")
    private Double longitude;

    @Schema(description = "Whether the observation has been verified", example = "false")
    private boolean checked;

    @Schema(description = "Whether a revision has been requested", example = "false")
    private boolean revisionRequested;

    @Schema(description = "Common name of the identified species", example = "English Oak")
    private String speciesCommonName;

    @Schema(description = "Certainty score of the identification (0-1)", example = "0.95")
    private Double certainty;

    @Schema(description = "Similarity score from AI analysis (0-1)", example = "0.92")
    private Double similarityScore;

    @Schema(description = "Type of observation", example = "PLANT", allowableValues = {"PLANT", "ANIMAL", "FUNGI", "UNKNOWN"})
    private ObservationType observationType;

    @Schema(description = "Associated specimen data (if applicable)")
    private SpecimenResponse specimen;

    @Schema(description = "URL to the observation image", example = "https://s3.example.com/uploads/observation/abc123.jpg")
    private String imageUrl;

    @Schema(description = "Timestamp when the observation was created", example = "2026-06-15T10:00:00Z")
    private Instant createdAt;

    public static ObservationResponse from(Observation observation, S3UrlGenerator urlGenerator) {
        return ObservationResponse.builder()
            .id(observation.getId())
            .latitude(observation.getLatitude())
            .longitude(observation.getLongitude())
            .checked(observation.isChecked())
            .revisionRequested(observation.isRevisionRequested())
            .speciesCommonName(observation.getSpeciesCommonName())
            .certainty(observation.getCertainty())
            .similarityScore(observation.getSimilarityScore())
            .observationType(observation.getObservationType())
            .imageUrl(urlGenerator.generateUrl(observation.getReferencePictureKey()))
            .specimen(observation.getSpecimen() != null ? SpecimenResponse.from(observation.getSpecimen()) : null)
            .createdAt(observation.getCreatedAt())
            .build();
    }
}
