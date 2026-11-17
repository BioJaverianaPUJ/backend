package com.biojaveriana.api.user.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated response containing a list of users", example = "{\"content\": [], \"pagination\": {\"page\": 0, \"size\": 20, \"totalElements\": 0, \"totalPages\": 0, \"first\": true, \"last\": true}}")
public record UserListResponse(
    @Schema(description = "List of user profiles in this page", example = "[{\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"firstName\": \"John\", \"lastName\": \"Doe\", \"username\": \"john_doe\", \"email\": \"john@example.com\", \"role\": \"USER\", \"gender\": \"MALE\", \"profilePictureUrl\": null, \"bannerUrl\": null}]")
    List<UserResponse> content,

    @Schema(description = "Pagination metadata for this page")
    Pagination pagination
) {
    public static UserListResponse from(Page<User> page) {
        List<UserResponse> content = page.getContent().stream()
            .map(UserResponse::from)
            .toList();

        return new UserListResponse(
            content,
            Pagination.from(page)
        );
    }
}
