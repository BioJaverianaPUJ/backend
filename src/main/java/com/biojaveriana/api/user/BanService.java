package com.biojaveriana.api.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.user.exceptions.BanNotFoundException;
import com.biojaveriana.api.user.exceptions.UserDeactivatedException;
import com.biojaveriana.api.user.exceptions.UserNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class BanService {
    private final BanRepository banRepository;
    private final UserRepository userRepository;

    public Ban createTemporalBan(UUID userId, String reason, Instant expiresAt, UUID createdBy) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Ban reason cannot be blank");
        }

        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Expiry date must be in the future");
        }

        User creator = userRepository.findById(createdBy)
            .orElseThrow(() -> new UserNotFoundException(createdBy));

        Ban ban = Ban.builder()
            .user(user)
            .reason(reason)
            .banType(BanType.TEMPORAL)
            .status(BanStatus.ACTIVE)
            .expiresAt(expiresAt)
            .createdBy(creator)
            .build();

        return banRepository.save(ban);
    }

    public Ban createPermanentBan(UUID userId, String reason, UUID createdBy) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Ban reason cannot be blank");
        }

        User creator = userRepository.findById(createdBy)
            .orElseThrow(() -> new UserNotFoundException(createdBy));

        Ban ban = Ban.builder()
            .user(user)
            .reason(reason)
            .banType(BanType.PERMANENT)
            .status(BanStatus.ACTIVE)
            .createdBy(creator)
            .build();

        return banRepository.save(ban);
    }

    public Ban cancelBan(UUID banId) {
        Ban ban = banRepository.findById(banId)
            .orElseThrow(() -> new BanNotFoundException(banId));

        ban.setStatus(BanStatus.CANCELLED);
        return banRepository.save(ban);
    }

    @Transactional(readOnly = true)
    public Optional<Ban> findActiveBanByUserId(UUID userId) {
        return banRepository.findActiveBanByUserId(userId);
    }

    public void checkActiveBan(UUID userId) {
        Optional<Ban> activeBan = findActiveBanByUserId(userId);
        activeBan.ifPresent(ban -> {
            if (ban.getBanType() == BanType.TEMPORAL && ban.getExpiresAt() != null
                && ban.getExpiresAt().isBefore(Instant.now())) {
                ban.setStatus(BanStatus.EXPIRED);
                banRepository.save(ban);
            } else {
                throw new UserDeactivatedException();
            }
        });
    }

    @Transactional(readOnly = true)
    public List<Ban> findAllBans() {
        return banRepository.findByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Ban> findBansByUserId(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        return banRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Ban findBanById(UUID banId) {
        return banRepository.findById(banId)
            .orElseThrow(() -> new BanNotFoundException(banId));
    }
}