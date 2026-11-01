package com.biojaveriana.api.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmUploadRequest(
        @NotBlank String key
) {}
