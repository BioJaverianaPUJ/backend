package com.biojaveriana.api.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.biojaveriana.api.SecuritySliceConfiguration;
import com.biojaveriana.api.auth.exceptions.InvalidCredentialsException;
import com.biojaveriana.api.user.BanService;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;
import com.biojaveriana.api.user.exceptions.UsernameAlreadyExistsException;

@WebMvcTest(AuthController.class)
@Import(SecuritySliceConfiguration.class)
class AuthControllerTest {

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private BanService banService;

    @Autowired
    private MockMvc mockMvc;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).username("jane").email("jane@example.com").role(Role.USER).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(jwt, "N/A", List.of()));
        when(userService.findById(userId)).thenReturn(user);
    }

    private Session session() {
        return new Session(user, new Token("jwt-value", Instant.now().plusSeconds(60)));
    }

    @Test
    @DisplayName("POST /login returns 200 with the user and a fresh token")
    void loginReturnsToken() throws Exception {
        when(authService.login("jane@example.com", "correct-password-1")).thenReturn(session());

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "jane@example.com", "password": "correct-password-1"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.email").value("jane@example.com"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.token").value("jwt-value"));
    }

    @Test
    @DisplayName("POST /login returns 401 for invalid credentials")
    void loginInvalidCredentials() throws Exception {
        when(authService.login("jane@example.com", "wrong-password-1")).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "jane@example.com", "password": "wrong-password-1"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /login returns 400 for a malformed body")
    void loginValidationFails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "not-an-email", "password": "short"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }


    @Test
    @DisplayName("POST /register returns 201 with the initial token")
    void registerReturnsCreated() throws Exception {
        when(authService.register(any())).thenReturn(session());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "Jane",
                      "lastName": "Doe",
                      "username": "jane",
                      "email": "jane@example.com",
                      "gender": "FEMALE",
                      "password": "correct-password-1"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("jwt-value"));
    }

    @Test
    @DisplayName("POST /register returns 409 for a taken username")
    void registerConflict() throws Exception {
        when(authService.register(any())).thenThrow(new UsernameAlreadyExistsException("jane"));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "Jane",
                      "lastName": "Doe",
                      "username": "jane",
                      "email": "jane@example.com",
                      "gender": "FEMALE",
                      "password": "correct-password-1"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("POST /refresh-token returns 401 without authentication")
    void refreshTokenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /refresh-token returns a fresh token for the authenticated user")
    void refreshTokenReturnsFreshToken() throws Exception {
        authenticate();
        when(authService.refreshToken(user)).thenReturn(session());

        mockMvc.perform(post("/api/v1/auth/refresh-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-value"))
            .andExpect(jsonPath("$.userId").value(userId.toString()));
    }
}
