package com.biojaveriana.api.user;

import java.io.IOException;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

@RestController
@RequestMapping("/api/v1/users")
@Validated
@RequiredArgsConstructor
public class UserController {
    private static final String SUPERADMIN_USERNAME = "superadmin";

    private final UserService userService;
    private final ObjectService objectService;

    @GetMapping("/me")
    public UserResponse getCurrentUser(@CurrentUser User user) {
        return UserResponse.from(user, objectService);
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable String id) {
        var user = userService.findById(UUID.fromString(id));
        return UserResponse.from(user, objectService);
    }

    @GetMapping("/@{username}")
    public UserResponse getUserByAtUsername(@PathVariable String username) {
        var user = userService.findByUsername(username);
        return UserResponse.from(user, objectService);
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
        return UserResponse.from(user, objectService);
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@CurrentUser User user, @Valid @RequestBody MeUserUpdateRequest dto) {
        var updated = userService.updateMe(user, dto);
        return UserResponse.from(updated, objectService);
    }

    @PatchMapping("/me/media")
    public UserResponse uploadMedia(
        @CurrentUser User user,
        @RequestParam String type,
        @RequestPart("file") @Valid MultipartFile file
    ) throws java.io.IOException {
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

    @DeleteMapping("/me/media")
    public UserResponse deleteMedia(
        @CurrentUser User user,
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

        return UserResponse.from(user, objectService);
    }

    @PatchMapping("/@{username}/media")
    @HasRole(value = Role.ADMIN)
    public UserResponse uploadMediaForAdmin(
        @PathVariable String username,
        @RequestParam String type,
        @RequestPart("file") @Valid MultipartFile file
    ) throws java.io.IOException {
        var user = userService.findByUsername(username);
        return uploadMediaInternal(user, type, file);
    }

    @DeleteMapping("/@{username}/media")
    @HasRole(value = Role.ADMIN)
    public UserResponse deleteMediaForAdmin(
        @PathVariable String username,
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

        return UserResponse.from(user, objectService);
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
    public void delete(@PathVariable String id) {
        var user = userService.findById(UUID.fromString(id));
        if (SUPERADMIN_USERNAME.equals(user.getUsername())) {
            throw new SuperAdminDeletionException();
        }

        userService.deleteById(UUID.fromString(id));
    }
}
