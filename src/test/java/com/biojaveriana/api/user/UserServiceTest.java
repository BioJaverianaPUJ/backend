package com.biojaveriana.api.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.biojaveriana.api.user.dto.ChangePasswordRequest;
import com.biojaveriana.api.user.dto.UserUpdateRequest;
import com.biojaveriana.api.user.exceptions.InvalidCurrentPasswordException;
import com.biojaveriana.api.user.exceptions.NewPasswordSameAsCurrentException;
import com.biojaveriana.api.user.exceptions.SuperAdminDeletionException;
import com.biojaveriana.api.user.exceptions.SuperAdminRoleModificationException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("re-hashes and saves the password when the current password matches")
        void shouldChangePassword() {
            User user = mock(User.class);
            when(user.getPasswordHash()).thenReturn("old-hash");
            when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
            when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
            when(userRepository.save(user)).thenReturn(user);

            User result = userService.changePassword(user, new ChangePasswordRequest("old-password", "new-password"));

            assertSame(user, result);
            verify(user).setPasswordHash("new-hash");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("throws when the current password does not match")
        void shouldRejectWrongCurrentPassword() {
            User user = mock(User.class);
            when(user.getPasswordHash()).thenReturn("old-hash");
            when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

            assertThrows(InvalidCurrentPasswordException.class,
                () -> userService.changePassword(user, new ChangePasswordRequest("wrong-password", "new-password")));

            verify(user, never()).setPasswordHash(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("throws when the new password is the same as the current one")
        void shouldRejectSamePassword() {
            User user = mock(User.class);
            when(user.getPasswordHash()).thenReturn("old-hash");
            when(passwordEncoder.matches("same-password", "old-hash")).thenReturn(true);

            assertThrows(NewPasswordSameAsCurrentException.class,
                () -> userService.changePassword(user, new ChangePasswordRequest("same-password", "same-password")));

            verify(user, never()).setPasswordHash(anyString());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("updateById")
    class UpdateByIdTests {

        @Test
        @DisplayName("rejects any update to the superadmin account")
        void shouldRejectSuperAdminUpdate() {
            UUID id = UUID.randomUUID();
            User superAdmin = mock(User.class);
            when(superAdmin.isSuperAdmin()).thenReturn(true);
            when(userRepository.findById(id)).thenReturn(Optional.of(superAdmin));

            assertThrows(SuperAdminRoleModificationException.class,
                () -> userService.updateById(id, new UserUpdateRequest(null, null, null, Role.ADMIN, null)));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("rejects promoting a user to SUPERADMIN")
        void shouldRejectPromotingToSuperAdmin() {
            UUID id = UUID.randomUUID();
            User user = mock(User.class);
            when(user.isSuperAdmin()).thenReturn(false);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            assertThrows(SuperAdminRoleModificationException.class,
                () -> userService.updateById(id, new UserUpdateRequest(null, null, null, Role.SUPERADMIN, null)));

            verify(user, never()).setRole(Role.SUPERADMIN);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("allows changing a regular user's role to ADMIN")
        void shouldUpdateRole() {
            UUID id = UUID.randomUUID();
            User user = mock(User.class);
            when(user.isSuperAdmin()).thenReturn(false);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);

            User result = userService.updateById(id, new UserUpdateRequest(null, null, null, Role.ADMIN, null));

            assertSame(user, result);
            verify(user).setRole(Role.ADMIN);
            verify(userRepository).save(user);
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteByIdTests {

        @Test
        @DisplayName("rejects deletion of the superadmin account")
        void shouldRejectSuperAdminDeletion() {
            UUID id = UUID.randomUUID();
            User superAdmin = mock(User.class);
            when(superAdmin.isSuperAdmin()).thenReturn(true);
            when(userRepository.findById(id)).thenReturn(Optional.of(superAdmin));

            assertThrows(SuperAdminDeletionException.class,
                () -> userService.deleteById(id));

            verify(userRepository, never()).delete(any(User.class));
        }

        @Test
        @DisplayName("deletes a regular user")
        void shouldDeleteRegularUser() {
            UUID id = UUID.randomUUID();
            User user = mock(User.class);
            when(user.isSuperAdmin()).thenReturn(false);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            userService.deleteById(id);

            verify(userRepository).delete(user);
        }
    }
}
