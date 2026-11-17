package com.biojaveriana.api.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Request body for updating an existing post", example = "{\"title\": \"Updated Title\", \"description\": \"Updated description\", \"observation\": \"Updated observation notes\"}")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostUpdateRequest {
    @Schema(description = "Updated title of the post (max 200 characters)", example = "Updated Title", maxLength = 200)
    private String title;

    @Schema(description = "Updated detailed description", example = "Updated description")
    private String description;

    @Schema(description = "Updated observation notes", example = "Updated observation notes")
    private String observation;
}
