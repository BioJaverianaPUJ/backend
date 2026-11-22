package com.biojaveriana.api.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
import com.biojaveriana.api.user.exceptions.InvalidCurrentPasswordException;
import com.biojaveriana.api.user.exceptions.NewPasswordSameAsCurrentException;

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
}
