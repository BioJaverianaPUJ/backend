package com.biojaveriana.api.comment.dto;

import java.util.UUID;

public record UserSummaryResponse(
    UUID id,
    String username,
    String firstName,
    String lastName,
    String profilePictureUrl
) {
}
