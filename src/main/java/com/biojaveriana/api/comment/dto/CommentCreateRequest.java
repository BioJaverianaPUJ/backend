package com.biojaveriana.api.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Request body for creating a comment", example = "{\"content\": \"This is a great observation!\", \"parentId\": \"550e8400-e29b-41d4-a716-446655440000\"}")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentCreateRequest {
    @Schema(description = "Content of the comment (required)", example = "This is a great observation!", maxLength = 5000)
    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Content must be at most 5000 characters")
    private String content;

    @Schema(description = "ID of the parent comment for replies (optional)", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID parentId;
}
