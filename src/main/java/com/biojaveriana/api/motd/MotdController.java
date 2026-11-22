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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/motd")
@Validated
@RequiredArgsConstructor
public class MotdController {
    private final FunFactService funFactService;
    private final MotdService motdService;

    @GetMapping("/today")
    public ResponseEntity<MotdResponse> today() {
        log.info("Fetching today's MOTD");
        return motdService.today()
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.ok(null));
    }

    @PostMapping("/funfacts/presign")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<UploadPresign> createPresign(
            @CurrentUser User user,
            @RequestBody @Valid FunFactImageUploadPresignRequest request) {
        return ResponseEntity.ok(funFactService.createPresign(user, request.getContentType()));
    }

    @PostMapping("/funfacts")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<FunFactResponse> create(
            @CurrentUser User user,
            @RequestBody @Valid FunFactCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funFactService.create(user, request));
    }

    @PutMapping("/funfacts/{funFactId}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<FunFactResponse> update(
            @PathVariable UUID funFactId,
            @CurrentUser User user,
            @RequestBody @Valid FunFactUpdateRequest request) {
        return ResponseEntity.ok(funFactService.update(funFactId, user, request));
    }

    @DeleteMapping("/funfacts/{funFactId}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<Void> delete(
            @PathVariable UUID funFactId,
            @CurrentUser User user) {
        funFactService.delete(funFactId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/funfacts/{funFactId}")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<FunFactResponse> getById(@PathVariable UUID funFactId) {
        return ResponseEntity.ok(funFactService.getById(funFactId));
    }

    @GetMapping("/funfacts")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<FunFactListResponse> list(
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        log.info("Listing fun facts for admin");
        return ResponseEntity.ok(funFactService.list(page, size));
    }

    @PutMapping("/funfacts/{funFactId}/toggle-hidden")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<FunFactResponse> toggleHidden(
            @PathVariable UUID funFactId,
            @CurrentUser User user) {
        return ResponseEntity.ok(funFactService.toggleHidden(funFactId, user));
    }
}
