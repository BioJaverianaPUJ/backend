package com.biojaveriana.api.user;

import java.io.IOException;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
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
import com.biojaveriana.api.user.dto.ChangePasswordRequest;
import com.biojaveriana.api.user.dto.DeleteAccountRequest;
import com.biojaveriana.api.user.dto.MeUserUpdateRequest;
import com.biojaveriana.api.user.dto.UserListResponse;
import com.biojaveriana.api.user.dto.UserUpdateRequest;
import com.biojaveriana.api.user.dto.UserResponse;
import com.biojaveriana.api.s3.ObjectService;
import com.biojaveriana.api.user.exceptions.InvalidMediaTypeException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles requests for managing users and their profile media.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@Validated
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profiles and administration. The /me endpoints operate on " +
    "the authenticated user; the other endpoints are admin-only and manage any user (profile " +
    "fields, role, media, account deletion). Profile pictures and banners are uploaded as " +
    "multipart files with `type=profile-picture` or `type=banner`.")
public class UserController {

    private final UserService userService;
    private final UserDeletionService userDeletionService;
    private final ObjectService objectService;

    @Operation(summary = "My profile",
            description = "Returns the authenticated user's own profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The authenticated user's profile"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(UserResponse.from(user, objectService));
    }

    @Operation(summary = "Get a user by ID",
            description = "Retrieves a user's public profile by their ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User successfully retrieved"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ID of the user to fetch", required = true) @PathVariable UUID id) {
        var user = userService.findById(id);
        return ResponseEntity.ok(UserResponse.from(user, objectService));
    }

    @Operation(summary = "Get a user by username",
            description = "Retrieves a user's public profile by their `@username`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User successfully retrieved"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/@{username}")
    public ResponseEntity<UserResponse> getUserByAtUsername(
            @Parameter(description = "Username (without the @ prefix)", required = true) @PathVariable String username) {
        var user = userService.findByUsername(username);
        return ResponseEntity.ok(UserResponse.from(user, objectService));
    }

    @Operation(summary = "List users",
            description = "Paginated list of users, sorted alphabetically by username.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of users successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    @GetMapping
    public ResponseEntity<UserListResponse> findAll(
        @Parameter(description = "Zero-based page index", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "Page size (1..100)", example = "20")
        @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        var pageable = PageRequest.of(
            page, size,
            Sort.by("username").ascending()
        );

        var userPage = userService.findAll(pageable);
        return ResponseEntity.ok(UserListResponse.from(userPage));
    }

    @Operation(summary = "Update a user (admin)",
            description = "Partially updates a user's profile fields and role; only non-null " +
                    "fields are applied. Requires the ADMIN role (SUPERADMIN bypasses the " +
                    "check). The role of a SUPERADMIN cannot be changed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User successfully updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed or an illegal operation (e.g. modifying a SUPERADMIN)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "429", description = "Username change cooldown still active")
    })
    @PatchMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<UserResponse> update(
            @Parameter(description = "ID of the user to update", required = true) @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest dto) {
        var user = userService.updateById(id, dto);
        return ResponseEntity.ok(UserResponse.from(user, objectService));
    }

    @Operation(summary = "Update my profile",
            description = "Partially updates the authenticated user's profile (first name, " +
                    "last name, username and gender); only non-null fields are applied.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile successfully updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @Parameter(hidden = true) @CurrentUser User user,
            @Valid @RequestBody MeUserUpdateRequest dto) {
        var updated = userService.updateMe(user, dto);
        return ResponseEntity.ok(UserResponse.from(updated, objectService));
    }

    @Operation(summary = "Change my password",
            description = "Changes the authenticated user's password. The current password " +
                    "must be provided and the new password must differ from it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password successfully changed"),
            @ApiResponse(responseCode = "400", description = "Invalid current password, new password too short, or same as current"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    @PatchMapping("/me/password")
    public ResponseEntity<UserResponse> changePassword(
        @Parameter(hidden = true) @CurrentUser User user,
        @Valid @RequestBody ChangePasswordRequest dto
    ) {
        var updated = userService.changePassword(user, dto);
        return ResponseEntity.ok(UserResponse.from(updated, objectService));
    }

    @Operation(summary = "Delete my account",
            description = "Deletes the authenticated user's own account after confirming the " +
                    "current password.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deleted"),
            @ApiResponse(responseCode = "400", description = "Wrong password"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(
        @Parameter(hidden = true) @CurrentUser User user,
        @Valid @RequestBody DeleteAccountRequest dto
    ) {
        log.info("Deleting user {} by their own request", user.getId());
        userDeletionService.deleteMyAccount(user, dto.password());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload my profile media",
            description = "Uploads a profile picture or banner for the authenticated user " +
            "(multipart form with a `file` part and `type=profile-picture` or `type=banner`). " +
            "Replaces any existing media of that type.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile with the updated media URL"),
            @ApiResponse(responseCode = "400", description = "Invalid or empty file"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "415", description = "Unknown media type")
    })
    @PatchMapping(value = "/me/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadMedia(
        @Parameter(hidden = true) @CurrentUser User user,
        @Parameter(description = "Media type: `profile-picture` or `banner`", required = true,
                schema = @Schema(allowableValues = {"profile-picture", "banner"}))
        @RequestParam String type,
        @Parameter(description = "Image file", required = true)
        @RequestPart("file") @Valid MultipartFile file
    ) throws IOException {
        return ResponseEntity.ok(uploadMediaInternal(user, type, file));
    }

    @Operation(summary = "Delete my profile media",
            description = "Removes the profile picture or banner of the authenticated user " +
            "(`type=profile-picture` or `type=banner`).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile with the media cleared"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "415", description = "Unknown media type")
    })
    @DeleteMapping("/me/media")
    public ResponseEntity<UserResponse> deleteMedia(
        @Parameter(hidden = true) @CurrentUser User user,
        @Parameter(description = "Media type: `profile-picture` or `banner`", required = true,
                schema = @Schema(allowableValues = {"profile-picture", "banner"}))
        @RequestParam String type
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

    @Operation(summary = "Upload a user's profile media (admin)",
            description = "Uploads a profile picture or banner on behalf of another user " +
            "(multipart form with a `file` part and `type=profile-picture` or `type=banner`). " +
            "Requires the ADMIN role (SUPERADMIN bypasses the check).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile with the updated media URL"),
            @ApiResponse(responseCode = "400", description = "Invalid or empty file"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "415", description = "Unknown media type")
    })
    @PatchMapping(value = "/@{username}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<UserResponse> uploadMediaForAdmin(
        @Parameter(description = "Username (without the @ prefix)", required = true) @PathVariable String username,
        @Parameter(description = "Media type: `profile-picture` or `banner`", required = true,
                schema = @Schema(allowableValues = {"profile-picture", "banner"}))
        @RequestParam String type,
        @Parameter(description = "Image file", required = true)
        @RequestPart("file") @Valid MultipartFile file
    ) throws IOException {
        var user = userService.findByUsername(username);
        return ResponseEntity.ok(uploadMediaInternal(user, type, file));
    }

    @Operation(summary = "Delete a user's profile media (admin)",
            description = "Removes the profile picture or banner of another user " +
            "(`type=profile-picture` or `type=banner`). Requires the ADMIN role " +
            "(SUPERADMIN bypasses the check).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile with the media cleared"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "415", description = "Unknown media type")
    })
    @DeleteMapping("/@{username}/media")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<UserResponse> deleteMediaForAdmin(
        @Parameter(description = "Username (without the @ prefix)", required = true) @PathVariable String username,
        @Parameter(description = "Media type: `profile-picture` or `banner`", required = true,
                schema = @Schema(allowableValues = {"profile-picture", "banner"}))
        @RequestParam String type
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

    @Operation(summary = "Delete a user (admin)",
            description = "Deletes another user's account and its data. Requires the ADMIN role " +
                    "(SUPERADMIN bypasses the check). SUPERADMIN accounts cannot be deleted.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin or target is a SUPERADMIN"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID of the user to delete", required = true) @PathVariable UUID id,
        @Parameter(hidden = true) @CurrentUser User admin
    ) {
        var user = userService.findById(id);

        log.info("Deleting user {} by admin id={}", id, admin.getId());
        userDeletionService.deleteAccount(user);
        return ResponseEntity.noContent().build();
    }
}
