package com.biojaveriana.api.auth;

import com.biojaveriana.api.auth.dto.RegistrationRequest;
import com.biojaveriana.api.auth.exceptions.InvalidCredentialsException;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserRepository;
import com.biojaveriana.api.user.exceptions.EmailAlreadyLinkedException;
import com.biojaveriana.api.user.exceptions.UsernameAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("login - Should return session when credentials are valid")
    void login_Success() {
        String email = "test@javeriana.edu.co";
        String rawPassword = "StrongPassword123!";
        String encodedHash = "encoded_hash_string";
        
        // Token envuelto en su clase o tipo correspondiente según tu backend
        Token mockToken = mock(Token.class);

        User mockUser = User.builder()
                .email(email)
                .passwordHash(encodedHash)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(rawPassword, encodedHash)).thenReturn(true);
        when(jwtService.generateToken(mockUser)).thenReturn(mockToken);

        Session session = authService.login(email, rawPassword);

        assertThat(session).isNotNull();
        assertThat(session.user()).isEqualTo(mockUser);
        assertThat(session.token()).isEqualTo(mockToken);
        
        verify(userRepository, times(1)).findByEmail(email);
        verify(passwordEncoder, times(1)).matches(rawPassword, encodedHash);
        verify(jwtService, times(1)).generateToken(mockUser);
    }

    @Test
    @DisplayName("login - Should throw InvalidCredentialsException when email does not exist")
    void login_ThrowsException_WhenEmailNotFound() {
        String email = "noexiste@javeriana.edu.co";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(email, "Password123!"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository, times(1)).findByEmail(email);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login - Should throw InvalidCredentialsException when password does not match")
    void login_ThrowsException_WhenPasswordDoesNotMatch() {
        String email = "test@javeriana.edu.co";
        String encodedHash = "encoded_hash_string";
        User mockUser = User.builder().email(email).passwordHash(encodedHash).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("WrongPassword", encodedHash)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(email, "WrongPassword"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder, times(1)).matches("WrongPassword", encodedHash);
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("refreshToken - Should generate and return a new session successfully")
    void refreshToken_Success() {
        User mockUser = User.builder().email("test@javeriana.edu.co").build();
        Token mockToken = mock(Token.class);

        when(jwtService.generateToken(mockUser)).thenReturn(mockToken);

        Session session = authService.refreshToken(mockUser);

        assertThat(session).isNotNull();
        assertThat(session.user()).isEqualTo(mockUser);
        assertThat(session.token()).isEqualTo(mockToken);

        verify(jwtService, times(1)).generateToken(mockUser);
    }

    @Test
    @DisplayName("register - Should register user successfully when request is valid")
    void register_Success() {
        // Orden correcto de los 6 parámetros del record RegistrationRequest
        RegistrationRequest request = new RegistrationRequest(
                "Juan", "Pérez", "juanp", "juan@javeriana.edu.co", null, "StrongPassword123!"
        );

        String encodedHash = "encoded_password_hash";
        Token mockToken = mock(Token.class);

        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn(encodedHash);
        when(jwtService.generateToken(any(User.class))).thenReturn(mockToken);

        Session session = authService.register(request);

        assertThat(session).isNotNull();
        assertThat(session.token()).isEqualTo(mockToken);
        assertThat(session.user().getUsername()).isEqualTo(request.username());
        assertThat(session.user().getEmail()).isEqualTo(request.email());
        assertThat(session.user().getPasswordHash()).isEqualTo(encodedHash);
        assertThat(session.user().getRole()).isEqualTo(Role.USER);

        verify(userRepository, times(1)).existsByUsername(request.username());
        verify(userRepository, times(1)).existsByEmail(request.email());
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtService, times(1)).generateToken(any(User.class));
    }

    @Test
    @DisplayName("register - Should throw UsernameAlreadyExistsException when username is taken")
    void register_ThrowsException_WhenUsernameExists() {
        RegistrationRequest request = new RegistrationRequest(
                "Juan", "Pérez", "juanp", "juan@javeriana.edu.co", null, "StrongPassword123!"
        );

        when(userRepository.existsByUsername(request.username())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, times(1)).existsByUsername(request.username());
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - Should throw EmailAlreadyLinkedException when email is taken")
    void register_ThrowsException_WhenEmailExists() {
        RegistrationRequest request = new RegistrationRequest(
                "Juan", "Pérez", "juanp", "juan@javeriana.edu.co", null, "StrongPassword123!"
        );

        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyLinkedException.class);

        verify(userRepository, times(1)).existsByUsername(request.username());
        verify(userRepository, times(1)).existsByEmail(request.email());
        verify(userRepository, never()).save(any());
    }
}