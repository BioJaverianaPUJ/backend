package com.biojaveriana.api.user;

import java.util.UUID;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.user.dto.BanResponse;
import com.biojaveriana.api.user.dto.CreateBanRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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

        return ResponseEntity.ok(BanResponse.from(ban));
    }

    @PatchMapping("/{id}/cancel")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<BanResponse> cancelBan(@CurrentUser User admin, @PathVariable UUID id) {
        Ban ban = banService.cancelBan(id);
        return ResponseEntity.ok(BanResponse.from(ban));
    }

    @GetMapping
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<List<BanResponse>> findAllBans() {
        return ResponseEntity.ok(
            banService.findAllBans().stream()
                .map(BanResponse::from)
                .collect(java.util.stream.Collectors.toList())
        );
    }

    @GetMapping("/user/{userId}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<List<BanResponse>> findBansByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(
            banService.findBansByUserId(userId).stream()
                .map(BanResponse::from)
                .collect(java.util.stream.Collectors.toList())
        );
    }
}