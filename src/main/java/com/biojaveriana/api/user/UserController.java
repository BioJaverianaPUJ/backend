package com.biojaveriana.api.user;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.user.dto.MeUserUpdateRequest;
import com.biojaveriana.api.user.dto.UserListResponse;
import com.biojaveriana.api.user.dto.UserUpdateRequest;
import com.biojaveriana.api.user.dto.UserResponse;
import com.biojaveriana.api.s3.S3UrlGenerator;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@Validated
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final S3UrlGenerator urlGenerator;

    @GetMapping("/me")
    public UserResponse getCurrentUser(@CurrentUser User user) {
        return UserResponse.from(user, urlGenerator);
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable String id) {
        var user = userService.findById(UUID.fromString(id));
        return UserResponse.from(user, urlGenerator);
    }

    @GetMapping("/@{username}")
    public UserResponse getUserByAtUsername(@PathVariable String username) {
        var user = userService.findByUsername(username);
        return UserResponse.from(user, urlGenerator);
    }

    @GetMapping
    public UserListResponse findAll(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        var pageable = PageRequest.of(
            page, size,
            Sort.by("username").ascending()
        );

        var userPage = userService.findAll(pageable);
        return UserListResponse.from(userPage);
    }

    @PatchMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public UserResponse update(@PathVariable String id, @Valid @RequestBody UserUpdateRequest dto) {
        var user = userService.updateById(UUID.fromString(id), dto);
        return UserResponse.from(user, urlGenerator);
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@CurrentUser User user, @Valid @RequestBody MeUserUpdateRequest dto) {
        var updated = userService.updateMe(user, dto);
        return UserResponse.from(updated, urlGenerator);
    }

    @DeleteMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public void delete(@PathVariable String id) {
        userService.deleteById(UUID.fromString(id));
    }
}
