package com.biojaveriana.api.user;

import java.io.IOException;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.user.dto.MeUserUpdateRequest;
import com.biojaveriana.api.user.dto.UserListResponse;
import com.biojaveriana.api.user.dto.UserUpdateRequest;
import com.biojaveriana.api.user.dto.UserResponse;
import com.biojaveriana.api.s3.ObjectService;
import com.biojaveriana.api.user.exceptions.SuperAdminDeletionException;
import com.biojaveriana.api.user.exceptions.InvalidMediaTypeException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@Validated
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints for user management, profile operations, and media handling")
public class UserController {
    private static final String SUPERADMIN_USERNAME = "superadmin";

    private final UserService userService;
    private final ObjectService objectService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieves the authenticated user's profile information")
    @ApiResponse(responseCode = "200", description = "User profile retrieved successfully")
    public ResponseEntity<UserResponse> getCurrentUser(@CurrentUser @Parameter(hidden = true) User user) {
        return ResponseEntity.ok(UserResponse.from(user, objectService));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user's profile by their unique ID")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ID of the user", required = true) @PathVariable UUID id) {
        var user = userService.findById(id);
        return ResponseEntity.ok(UserResponse.from(user, objectService));
    }

    @GetMapping("/@{username}")
    @Operation(summary = "Get user by username", description = "Retrieves a user's profile by their username (without @ prefix)")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserResponse> getUserByAtUsername(
            @Parameter(description = "Username (without @ prefix)", required = true) @PathVariable String username) {
        var user = userService.findByUsername(username);
        return ResponseEntity.ok(UserResponse.from(user, objectService));
    }

    @GetMapping
    @Operation(summary = "List all users", description = "Retrieves a paginated list of all users in the system")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    public ResponseEntity<UserListResponse> findAll(
            @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "20")) @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        var pageable = PageRequest.of(
            page, size,
            Sort.by("username").ascending()
        );

        var userPage = userService.findAll(pageable);
        return ResponseEntity.ok(UserListResponse.from(userPage));
    }

    @PatchMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Update user (Admin)", description = "Updates a user's information. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "User updated successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserResponse> update(
            @Parameter(description = "ID of the user to update", required = true) @PathVariable UUID id,
            @Parameter(description = "User update request body") @Valid @RequestBody UserUpdateRequest dto) {
        var user = userService.updateById(id, dto);
        return ResponseEntity.ok(UserResponse.from(user, objectService));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update current user profile", description = "Allows the authenticated user to update their own profile")
    @ApiResponse(responseCode = "200", description = "Profile updated successfully")
    public ResponseEntity<UserResponse> updateMe(
            @CurrentUser @Parameter(hidden = true) User user,
            @Parameter(description = "Profile update request body") @Valid @RequestBody MeUserUpdateRequest dto) {
        var updated = userService.updateMe(user, dto);
        return ResponseEntity.ok(UserResponse.from(updated, objectService));
    }

    @PatchMapping("/me/media")
    @Operation(summary = "Upload user media", description = "Uploads a profile picture or banner for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Media uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid media type")
    public ResponseEntity<UserResponse> uploadMedia(
        @CurrentUser @Parameter(hidden = true) User user,
        @Parameter(description = "Media type (profile-picture or banner)", required = true) @RequestParam String type,
        @Parameter(description = "Media file", required = true) @RequestPart("file") @Valid MultipartFile file
    ) throws IOException {
        return ResponseEntity.ok(uploadMediaInternal(user, type, file));
    }

    @DeleteMapping("/me/media")
    @Operation(summary = "Delete user media", description = "Removes the user's profile picture or banner")
    @ApiResponse(responseCode = "200", description = "Media deleted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid media type")
    public ResponseEntity<UserResponse> deleteMedia(
        @CurrentUser @Parameter(hidden = true) User user,
        @Parameter(description = "Media type (profile-picture or banner)", required = true) @RequestParam String type
    ) {
        var mediaType = parseMediaType(type);
        if (mediaType == null) {
            throw new InvalidMediaTypeException(type);
        }

        var key = mediaType.getOldKey(user);
        if (key != null) {
            objectService.delete(key);
            mediaType.setKey(user, null);
            userService.save(user);
        }

        return ResponseEntity.ok(UserResponse.from(user, objectService));
    }

    @PatchMapping("/@{username}/media")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Upload media for user (Admin)", description = "Uploads media on behalf of another user. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Media uploaded successfully")
    public ResponseEntity<UserResponse> uploadMediaForAdmin(
        @Parameter(description = "Username of the target user", required = true) @PathVariable String username,
        @Parameter(description = "Media type (profile-picture or banner)", required = true) @RequestParam String type,
        @Parameter(description = "Media file", required = true) @RequestPart("file") @Valid MultipartFile file
    ) throws IOException {
        var user = userService.findByUsername(username);
        return ResponseEntity.ok(uploadMediaInternal(user, type, file));
    }

    @DeleteMapping("/@{username}/media")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Delete media for user (Admin)", description = "Removes media on behalf of another user. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Media deleted successfully")
    public ResponseEntity<UserResponse> deleteMediaForAdmin(
        @Parameter(description = "Username of the target user", required = true) @PathVariable String username,
        @Parameter(description = "Media type (profile-picture or banner)", required = true) @RequestParam String type
    ) {
        var user = userService.findByUsername(username);

        var mediaType = parseMediaType(type);
        if (mediaType == null) {
            throw new InvalidMediaTypeException(type);
        }

        var key = mediaType.getOldKey(user);
        if (key != null) {
            objectService.delete(key);
            mediaType.setKey(user, null);
            userService.save(user);
        }

        return ResponseEntity.ok(UserResponse.from(user, objectService));
    }

    private Media parseMediaType(String type) {
        if ("profile-picture".equals(type)) {
            return Media.PROFILE_PICTURE;
        }
        if ("banner".equals(type)) {
            return Media.BANNER;
        }

        return null;
    }

    private UserResponse uploadMediaInternal(User user, String type, MultipartFile file) throws IOException {
        var mediaType = parseMediaType(type);
        if (mediaType == null) {
            throw new InvalidMediaTypeException(type);
        }

        var oldKey = mediaType.getOldKey(user);
        if (oldKey != null) {
            objectService.delete(oldKey);
        }

        String newKey = mediaType.generateKey(user, file.getContentType());
        objectService.upload(newKey, file.getInputStream(), file.getSize(), file.getContentType());
        mediaType.setKey(user, newKey);
        userService.save(user);

        return UserResponse.from(user, objectService);
    }

    @DeleteMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Delete user (Admin)", description = "Permanently deletes a user from the system. Admin-only operation.")
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @ApiResponse(responseCode = "403", description = "Cannot delete superadmin user")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID of the user to delete", required = true) @PathVariable UUID id,
        @CurrentUser @Parameter(hidden = true) User admin
    ) {
        var user = userService.findById(id);
        if (SUPERADMIN_USERNAME.equals(user.getUsername())) {
            throw new SuperAdminDeletionException();
        }

        log.info("Deleting user {} by admin id={}", id, admin.getId());
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
