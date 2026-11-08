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
public class BanController {
    private final BanService banService;

    @PostMapping
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<BanResponse> createBan(
        @CurrentUser User admin,
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

    @PatchMapping("/{id}/cancel")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<BanResponse> cancelBan(@CurrentUser User admin, @PathVariable UUID id) {
        Ban ban = banService.cancelBan(id);
        return ResponseEntity.ok(BanResponse.from(ban));
    }

    @GetMapping
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<BanListResponse> findAllBans(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Max(100) int size
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
    public ResponseEntity<BanListResponse> findBansByUser(
        @PathVariable UUID userId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
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