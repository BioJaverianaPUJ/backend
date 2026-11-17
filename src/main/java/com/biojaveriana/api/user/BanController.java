package com.biojaveriana.api.user;

import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/bans")
@Validated
@RequiredArgsConstructor
@Tag(name = "Bans", description = "Endpoints for managing user bans and suspensions (Admin-only)")
public class BanController {
    private final BanService banService;

    @PostMapping
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Create a ban", description = "Creates a new ban (temporary or permanent) for a user. Admin-only operation.")
    @ApiResponse(responseCode = "201", description = "Ban created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid ban request data")
    public ResponseEntity<BanResponse> createBan(
        @CurrentUser @Parameter(hidden = true) User admin,
        @Parameter(description = "Ban creation request body", required = true) @Valid @RequestBody CreateBanRequest request
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

    @PatchMapping("/{id}/cancel")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Cancel a ban", description = "Cancels an existing ban. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Ban cancelled successfully")
    @ApiResponse(responseCode = "404", description = "Ban not found")
    public ResponseEntity<BanResponse> cancelBan(
        @CurrentUser @Parameter(hidden = true) User admin,
        @Parameter(description = "ID of the ban to cancel", required = true) @PathVariable UUID id
    ) {
        Ban ban = banService.cancelBan(id);
        return ResponseEntity.ok(BanResponse.from(ban));
    }

    @GetMapping
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "List all bans", description = "Retrieves a paginated list of all bans. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Bans retrieved successfully")
    public ResponseEntity<BanListResponse> findAllBans(
        @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "20")) @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        var pageable = PageRequest.of(
            page, size,
            Sort.by("createdAt").descending()
        );
        var banPage = banService.findAllBansPageable(pageable);
        return ResponseEntity.ok(BanListResponse.from(banPage));
    }

    @GetMapping("/user/{userId}")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "List bans for a user", description = "Retrieves all bans for a specific user. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Bans retrieved successfully")
    public ResponseEntity<BanListResponse> findBansByUser(
        @Parameter(description = "ID of the user", required = true) @PathVariable UUID userId,
        @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(0) int page,
        @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "20")) @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        var pageable = PageRequest.of(
            page, size,
            Sort.by("createdAt").descending()
        );
        var banPage = banService.findBansByUserIdPageable(userId, pageable);
        return ResponseEntity.ok(BanListResponse.from(banPage));
    }
}