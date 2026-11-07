package com.biojaveriana.api.post.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostImageUploadPresignRequest {
    @NotBlank(message = "Content type is required")
    private String contentType;
}
