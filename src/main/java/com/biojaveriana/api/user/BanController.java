package com.biojaveriana.api.user;

import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.user.dto.BanListResponse;
import com.biojaveriana.api.user.dto.BanResponse;
import com.biojaveriana.api.user.dto.CreateBanRequest;

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
 * Admin-only ban management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bans")
@Validated
@RequiredArgsConstructor
@Tag(name = "Bans", description = "User bans (temporal or permanent), managed by admins. " +
    "Banned users are rejected by the authentication filter on every request.")
public class BanController {
    private final BanService banService;

    @Operation(summary = "Ban a user (admin)",
        description = "Bans the given user. `banType` is `TEMPORAL` (requires `expiresAt`) or " +
            "`PERMANENT`. Banning a SUPERADMIN is forbidden. Requires the ADMIN role " +
            "(SUPERADMIN bypasses).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Ban created"),
        @ApiResponse(responseCode = "400", description = "Validation failed (missing expiresAt for TEMPORAL bans, ...)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Target user is a SUPERADMIN"),
        @ApiResponse(responseCode = "404", description = "User not found"),
    })
    @PostMapping
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<BanResponse> createBan(
        @Parameter(hidden = true) @CurrentUser User admin,
        @Valid @RequestBody CreateBanRequest request
    ) {
        Ban ban;

        if (request.banType() == CreateBanRequest.BanTypeRequest.TEMPORAL) {
            if (request.expiresAt() == null) {
                throw new IllegalArgumentException("expiresAt is required for TEMPORAL bans");
            }
            ban = banService.createTemporalBan(
                request.userId(),
                request.reason(),
                request.expiresAt(),
                admin.getId()
            );
        } else {
            ban = banService.createPermanentBan(
                request.userId(),
                request.reason(),
                admin.getId()
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(BanResponse.from(ban));
    }

    @Operation(summary = "Cancel a ban (admin)",
        description = "Cancels an active ban, restoring the user's access. Returns the ban with " +
            "its cancelled state. Requires the ADMIN role (SUPERADMIN bypasses).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The ban with its new cancelled state"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Ban not found"),
    })
    @PatchMapping("/{id}/cancel")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<BanResponse> cancelBan(
            @Parameter(hidden = true) @CurrentUser User admin,
            @Parameter(description = "ID of the ban to cancel", required = true) @PathVariable UUID id) {
        Ban ban = banService.cancelBan(id);
        return ResponseEntity.ok(BanResponse.from(ban));
    }

    @Operation(summary = "List all bans (admin)",
        description = "Paginated list of every ban, newest first. Requires the ADMIN role " +
            "(SUPERADMIN bypasses).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of bans"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
    })
    @GetMapping
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<BanListResponse> findAllBans(
        @Parameter(description = "Zero-based page index", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "Page size (1..100)", example = "20")
        @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        var pageable = PageRequest.of(
            page, size,
            Sort.by("createdAt").descending()
        );
        var banPage = banService.findAllBansPageable(pageable);
        return ResponseEntity.ok(BanListResponse.from(banPage));
    }

    @Operation(summary = "List a user's bans (admin)",
        description = "Paginated list of all bans on the given user, newest first. Requires the " +
            "ADMIN role (SUPERADMIN bypasses).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of bans"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
    })
    @GetMapping("/user/{userId}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<BanListResponse> findBansByUser(
        @Parameter(description = "ID of the user", required = true) @PathVariable UUID userId,
        @Parameter(description = "Zero-based page index", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "Page size (1..100)", example = "20")
        @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        var pageable = PageRequest.of(
            page, size,
            Sort.by("createdAt").descending()
        );
        var banPage = banService.findBansByUserIdPageable(userId, pageable);
        return ResponseEntity.ok(BanListResponse.from(banPage));
    }
}