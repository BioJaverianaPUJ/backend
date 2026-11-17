package com.biojaveriana.api.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Request body for updating a comment", example = "{\"content\": \"Updated comment content\"}")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentUpdateRequest {
    @Schema(description = "Updated content of the comment (required)", example = "Updated comment content", maxLength = 5000)
    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Content must be at most 5000 characters")
    private String content;
}
