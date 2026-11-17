package com.biojaveriana.api.observation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Request body for creating a new observation", example = "{\"referencePictureKey\": \"uploads/observation/abc123.jpg\", \"latitude\": 4.5708, \"longitude\": -75.7521, \"waitTimeoutSeconds\": 30}")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObservationCreateRequest {
    @Schema(description = "S3 key of the reference picture", example = "uploads/observation/abc123.jpg")
    @NotNull
    private String referencePictureKey;

    @Schema(description = "Latitude of the observation location (-90 to 90)", example = "4.5708")
    @NotNull
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @Schema(description = "Longitude of the observation location (-180 to 180)", example = "-75.7521")
    @NotNull
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @Schema(description = "Timeout for AI analysis in seconds (default: 30)", example = "30")
    @Default
    private Integer waitTimeoutSeconds = 30;
}
