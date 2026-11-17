package com.biojaveriana.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for confirming file upload via presigned URL", example = "{\"key\": \"uploads/profile/abc123.jpg\"}")
public record ConfirmUploadRequest(
    @Schema(description = "S3 object key of the uploaded file", example = "uploads/profile/abc123.jpg")
    @NotBlank
    String key
) {}
