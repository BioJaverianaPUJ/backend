package com.biojaveriana.api.user.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.user.User;

public record UserListResponse(
    List<UserResponse> content,
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
