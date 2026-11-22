package com.biojaveriana.api.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.biojaveriana.api.user.exceptions.SuperAdminBanException;
import com.biojaveriana.api.user.exceptions.UserDeactivatedException;
import com.biojaveriana.api.user.exceptions.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class BanServiceTest {

    @Mock
    private BanRepository banRepository;

    @Mock
    private UserRepository userRepository;

    private BanService banService;

    @BeforeEach
    void setUp() {
        banService = new BanService(banRepository, userRepository);
    }

    private User mockUser(Role role) {
        User user = mock(User.class);
        when(user.isSuperAdmin()).thenReturn(role == Role.SUPERADMIN);
        return user;
    }

    private Ban activeBan(BanType type, Instant expiresAt) {
        return Ban.builder()
            .banType(type)
            .status(BanStatus.ACTIVE)
            .expiresAt(expiresAt)
            .build();
    }

    @Nested
    @DisplayName("createTemporalBan")
    class CreateTemporalBanTests {

        @Test
        @DisplayName("rejects banning the superadmin account")
        void shouldRejectSuperAdmin() {
            UUID targetId = UUID.randomUUID();
            UUID creatorId = UUID.randomUUID();
            User superAdmin = mockUser(Role.SUPERADMIN);
            when(userRepository.findById(targetId)).thenReturn(Optional.of(superAdmin));

            assertThrows(SuperAdminBanException.class,
                () -> banService.createTemporalBan(targetId, "spam", Instant.now().plusSeconds(3600), creatorId));

            verify(banRepository, never()).save(any(Ban.class));
        }

        @Test
        @DisplayName("creates a temporal ban for a regular user")
        void shouldCreateTemporalBan() {
            UUID targetId = UUID.randomUUID();
            UUID creatorId = UUID.randomUUID();
            Instant expiresAt = Instant.now().plusSeconds(3600);
            User target = mockUser(Role.USER);
            User creator = mock(User.class);

            when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
            when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
            when(banRepository.save(any(Ban.class))).thenAnswer(inv -> inv.getArgument(0));

            Ban ban = banService.createTemporalBan(targetId, "spam", expiresAt, creatorId);

            assertEquals(BanType.TEMPORAL, ban.getBanType());
            assertEquals(BanStatus.ACTIVE, ban.getStatus());
            assertEquals(expiresAt, ban.getExpiresAt());
            assertSame(target, ban.getUser());
            assertSame(creator, ban.getCreatedBy());
            verify(banRepository).save(ban);
        }
    }

    @Nested
    @DisplayName("createPermanentBan")
    class CreatePermanentBanTests {

        @Test
        @DisplayName("rejects banning the superadmin account")
        void shouldRejectSuperAdmin() {
            UUID targetId = UUID.randomUUID();
            UUID creatorId = UUID.randomUUID();
            User superAdmin = mockUser(Role.SUPERADMIN);
            when(userRepository.findById(targetId)).thenReturn(Optional.of(superAdmin));

            assertThrows(SuperAdminBanException.class,
                () -> banService.createPermanentBan(targetId, "abuse", creatorId));

            verify(banRepository, never()).save(any(Ban.class));
        }

        @Test
        @DisplayName("creates a permanent ban for a regular user")
        void shouldCreatePermanentBan() {
            UUID targetId = UUID.randomUUID();
            UUID creatorId = UUID.randomUUID();
            User target = mockUser(Role.USER);
            User creator = mock(User.class);

            when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
            when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
            when(banRepository.save(any(Ban.class))).thenAnswer(inv -> inv.getArgument(0));

            Ban ban = banService.createPermanentBan(targetId, "abuse", creatorId);

            assertEquals(BanType.PERMANENT, ban.getBanType());
            assertEquals(BanStatus.ACTIVE, ban.getStatus());
            assertNull(ban.getExpiresAt());
            assertSame(target, ban.getUser());
            assertSame(creator, ban.getCreatedBy());
            verify(banRepository).save(ban);
        }
    }

    @Nested
    @DisplayName("checkActiveBan")
    class CheckActiveBanTests {

        @Test
        @DisplayName("throws when a regular user has an active ban")
        void shouldThrowForBannedUser() {
            UUID userId = UUID.randomUUID();
            User user = mockUser(Role.USER);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(banRepository.findActiveBanByUserId(userId))
                .thenReturn(Optional.of(activeBan(BanType.PERMANENT, null)));

            assertThrows(UserDeactivatedException.class, () -> banService.checkActiveBan(userId));
        }

        @Test
        @DisplayName("skips the ban check entirely for the superadmin")
        void shouldSkipSuperAdmin() {
            UUID userId = UUID.randomUUID();
            User superAdmin = mockUser(Role.SUPERADMIN);
            when(userRepository.findById(userId)).thenReturn(Optional.of(superAdmin));

            banService.checkActiveBan(userId);

            verify(banRepository, never()).findActiveBanByUserId(any());
        }

        @Test
        @DisplayName("throws when the user no longer exists")
        void shouldThrowUserNotFound() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> banService.checkActiveBan(userId));
        }

        @Test
        @DisplayName("marks an expired temporal ban as expired instead of throwing")
        void shouldMarkExpiredTemporalBan() {
            UUID userId = UUID.randomUUID();
            User user = mockUser(Role.USER);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            Ban expired = activeBan(BanType.TEMPORAL, Instant.now().minusSeconds(1));
            when(banRepository.findActiveBanByUserId(userId)).thenReturn(Optional.of(expired));
            when(banRepository.save(expired)).thenReturn(expired);

            banService.checkActiveBan(userId);

            assertEquals(BanStatus.EXPIRED, expired.getStatus());
            verify(banRepository).save(expired);
        }
    }
}