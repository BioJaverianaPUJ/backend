package com.biojaveriana.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.biojaveriana.api.auth.dto.RegistrationRequest;
import com.biojaveriana.api.auth.exceptions.InvalidCredentialsException;
import com.biojaveriana.api.user.Gender;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserRepository;
import com.biojaveriana.api.user.exceptions.EmailAlreadyLinkedException;
import com.biojaveriana.api.user.exceptions.UsernameAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
        user = User.builder()
            .id(UUID.randomUUID())
            .username("jane")
            .email("jane@example.com")
            .passwordHash("encoded-hash")
            .firstName("Jane")
            .lastName("Doe")
            .gender(Gender.FEMALE)
            .role(Role.USER)
            .build();
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("valid credentials return a session containing the user and a fresh token")
        void validCredentialsReturnSession() {
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("correct-password-1", user.getPasswordHash())).thenReturn(true);
            Token token = new Token("jwt-value", Instant.now());
            when(jwtService.generateToken(user)).thenReturn(token);

            Session session = authService.login("jane@example.com", "correct-password-1");

            assertSame(user, session.user());
            assertSame(token, session.token());
        }

        @Test
        @DisplayName("unknown email is rejected without issuing a token")
        void unknownEmailThrows() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThrows(InvalidCredentialsException.class,
                () -> authService.login("ghost@example.com", "correct-password-1"));

            verify(jwtService, never()).generateToken(any(User.class));
        }

        @Test
        @DisplayName("wrong password is rejected without issuing a token")
        void wrongPasswordThrows() {
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong-password-1", user.getPasswordHash())).thenReturn(false);

            assertThrows(InvalidCredentialsException.class,
                () -> authService.login("jane@example.com", "wrong-password-1"));

            verify(jwtService, never()).generateToken(any(User.class));
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("issues a fresh token for the given user")
        void refreshTokenIssuesFreshToken() {
            Token token = new Token("fresh-jwt", Instant.now());
            when(jwtService.generateToken(user)).thenReturn(token);

            Session session = authService.refreshToken(user);

            assertSame(user, session.user());
            assertSame(token, session.token());
        }
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        private RegistrationRequest request() {
            return new RegistrationRequest("Jane", "Doe", "jane", "jane@example.com", Gender.FEMALE, "s3cret-password");
        }

        @Test
        @DisplayName("new account is saved with an encoded password and the USER role")
        void registerSavesNewAccount() {
            when(userRepository.existsByUsername("jane")).thenReturn(false);
            when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
            when(passwordEncoder.encode("s3cret-password")).thenReturn("encoded-new-hash");
            Token token = new Token("jwt-value", Instant.now());
            when(jwtService.generateToken(any(User.class))).thenReturn(token);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            Session session = authService.register(request());

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertEquals("jane", saved.getUsername());
            assertEquals("jane@example.com", saved.getEmail());
            assertEquals("encoded-new-hash", saved.getPasswordHash());
            assertEquals(Role.USER, saved.getRole());
            assertSame(token, session.token());
        }

        @Test
        @DisplayName("the reserved username 'superadmin' is rejected regardless of the database")
        void registerRejectsReservedSuperadminUsername() {
            RegistrationRequest request =
                new RegistrationRequest("A", "B", "superadmin", "a@example.com", Gender.MALE, "s3cret-password");

            assertThrows(UsernameAlreadyExistsException.class, () -> authService.register(request));

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("a taken username is rejected before the email is checked")
        void registerRejectsTakenUsername() {
            when(userRepository.existsByUsername("jane")).thenReturn(true);

            assertThrows(UsernameAlreadyExistsException.class, () -> authService.register(request()));

            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("a taken email is rejected and nothing is saved")
        void registerRejectsTakenEmail() {
            when(userRepository.existsByUsername("jane")).thenReturn(false);
            when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

            assertThrows(EmailAlreadyLinkedException.class, () -> authService.register(request()));

            verify(userRepository, never()).save(any(User.class));
        }
    }
}

