package com.biojaveriana.api.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Request body for creating a new post", example = "{\"title\": \"New Species Discovered\", \"description\": \"Found a rare orchid in the campus forest\", \"observation\": \"The specimen appears to be a previously unrecorded variety\", \"imageKeys\": [\"uploads/posts/img1.jpg\", \"uploads/posts/img2.jpg\"]}")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreateRequest {
    @Schema(description = "Title of the post (max 200 characters)", example = "New Species Discovered", maxLength = 200)
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    @Schema(description = "Detailed description of the post", example = "Found a rare orchid in the campus forest")
    @NotBlank(message = "Description is required")
    private String description;

    @Schema(description = "Additional observation notes", example = "The specimen appears to be a previously unrecorded variety")
    private String observation;

    @Schema(description = "Array of S3 image keys uploaded with the post", example = "[\"uploads/posts/img1.jpg\", \"uploads/posts/img2.jpg\"]")
    private String[] imageKeys;
}
