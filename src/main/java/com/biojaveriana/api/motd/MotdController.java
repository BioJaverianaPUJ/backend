package com.biojaveriana.api.motd;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.motd.dto.FunFactCreateRequest;
import com.biojaveriana.api.motd.dto.FunFactImageUploadPresignRequest;
import com.biojaveriana.api.motd.dto.FunFactListResponse;
import com.biojaveriana.api.motd.dto.FunFactResponse;
import com.biojaveriana.api.motd.dto.FunFactUpdateRequest;
import com.biojaveriana.api.motd.dto.MotdResponse;
import com.biojaveriana.api.s3.UploadPresign;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Message of the day (MOTD) and the fun facts behind it.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/motd")
@Validated
@RequiredArgsConstructor
@Tag(name = "MOTD", description = "Message of the day. The public endpoint returns today's fun fact; " +
    "the /funfacts sub-resource is the admin catalogue (CRUD, image presigns and visibility).")
public class MotdController {
    private final FunFactService funFactService;
    private final MotdService motdService;

    @Operation(summary = "Today's MOTD",
        description = "Returns the fun fact featured today. The body is `null` when no fun fact " +
            "is active for the current day.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Today's fun fact (may be null)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping("/today")
    public ResponseEntity<MotdResponse> today() {
        log.info("Fetching today's MOTD");
        return motdService.today()
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.ok(null));
    }

    @Operation(summary = "Create a presigned fun-fact image upload URL (admin)",
        description = "Returns a short-lived signed URL and object key for uploading a fun-fact " +
            "image directly to object storage; reference `key` in `imageKeys` when creating the " +
            "fun fact. Requires the ADMIN role (SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Presigned upload details"),
        @ApiResponse(responseCode = "400", description = "Validation failed or fun-fact image quota exceeded"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "415", description = "Unsupported image content type"),
    })
    @PostMapping("/funfacts/presign")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<UploadPresign> createPresign(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody @Valid FunFactImageUploadPresignRequest request) {
        return ResponseEntity.ok(funFactService.createPresign(user, request.getContentType()));
    }

    @Operation(summary = "Create a fun fact (admin)",
        description = "Adds a fun fact to the MOTD pool. `imageKeys` must reference already-uploaded " +
            "images (via the presign flow); `poiId` optionally anchors the fact to a POI. " +
            "Requires the ADMIN role (SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Fun fact created"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Referenced image key or POI not found"),
    })
    @PostMapping("/funfacts")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<FunFactResponse> create(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody @Valid FunFactCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funFactService.create(user, request));
    }

    @Operation(summary = "Update a fun fact (admin)",
        description = "Partially updates a fun fact (title, description and/or linked POI); only " +
            "non-null fields are applied. Requires the ADMIN role (SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fun fact updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Fun fact not found"),
    })
    @PutMapping("/funfacts/{funFactId}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<FunFactResponse> update(
            @Parameter(description = "ID of the fun fact to update", required = true) @PathVariable UUID funFactId,
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody @Valid FunFactUpdateRequest request) {
        return ResponseEntity.ok(funFactService.update(funFactId, user, request));
    }

    @Operation(summary = "Delete a fun fact (admin)",
        description = "Removes a fun fact from the MOTD pool. " +
            "Requires the ADMIN role (SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Fun fact deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Fun fact not found"),
    })
    @DeleteMapping("/funfacts/{funFactId}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the fun fact to delete", required = true) @PathVariable UUID funFactId,
            @Parameter(hidden = true) @CurrentUser User user) {
        funFactService.delete(funFactId, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a fun fact by ID (admin)",
        description = "Fetches a single fun fact. Requires the ADMIN role " +
            "(SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The fun fact"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Fun fact not found"),
    })
    @GetMapping("/funfacts/{funFactId}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<FunFactResponse> getById(
            @Parameter(description = "ID of the fun fact to fetch", required = true) @PathVariable UUID funFactId) {
        return ResponseEntity.ok(funFactService.getById(funFactId));
    }

    @Operation(summary = "List all fun facts (admin)",
        description = "Paginated list of the whole fun-fact pool. Requires the ADMIN role " +
            "(SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of fun facts"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
    })
    @GetMapping("/funfacts")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<FunFactListResponse> list(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (1..100)", example = "20")
            @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        log.info("Listing fun facts for admin");
        return ResponseEntity.ok(funFactService.list(page, size));
    }

    @Operation(summary = "Toggle fun-fact visibility (admin)",
        description = "Hides or un-hides a fun fact from the MOTD rotation. Requires the ADMIN role " +
            "(SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The fun fact with its new hidden state"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Fun fact not found"),
    })
    @PutMapping("/funfacts/{funFactId}/toggle-hidden")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<FunFactResponse> toggleHidden(
            @Parameter(description = "ID of the fun fact to toggle", required = true) @PathVariable UUID funFactId,
            @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(funFactService.toggleHidden(funFactId, user));
    }
}
