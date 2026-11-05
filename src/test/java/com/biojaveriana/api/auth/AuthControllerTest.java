package com.biojaveriana.api.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.biojaveriana.api.auth.dto.LoginRequest;
import com.biojaveriana.api.auth.dto.RegistrationRequest;
import com.biojaveriana.api.security.CurrentUserArgumentResolver;
import com.biojaveriana.api.user.Gender;
import com.biojaveriana.api.user.User;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;
    
    @MockitoBean
    private com.biojaveriana.api.user.UserService userService;

    @MockitoBean
    private CurrentUserArgumentResolver currentUserArgumentResolver;

    @Test
    @DisplayName("POST /api/v1/auth/login - Should login successfully with valid credentials")
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest("test@javeriana.edu.co", "StrongPassword123!");
        User user = user("test@javeriana.edu.co");
        Session mockSession = session(user);
        
        when(authService.login(request.email(), request.password())).thenReturn(mockSession);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.token").value("test-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should return 400 Bad Request when validation fails")
    void login_BadRequest_WhenValidationFails() throws Exception {
        LoginRequest request = new LoginRequest("correo-invalido", "123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should register successfully with valid request")
    void register_Success() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "Juan", "Pérez", "juanp", "newuser@javeriana.edu.co", Gender.MALE, "StrongPassword123!"
        );
        User user = user(request.email());
        Session mockSession = session(user);

        when(authService.register(any(RegistrationRequest.class))).thenReturn(mockSession);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(request.email()))
                .andExpect(jsonPath("$.token").value("test-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh-token - Should refresh token successfully")
    void refreshToken_Success() throws Exception {
        User user = user("test@javeriana.edu.co");
        when(userService.findById(user.getId())).thenReturn(user);
        when(authService.refreshToken(user)).thenReturn(session(user));
        doReturn(true).when(currentUserArgumentResolver).supportsParameter(any());
        doReturn(user).when(currentUserArgumentResolver)
                .resolveArgument(any(), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .with(jwt().jwt(jwt -> jwt.subject(user.getId().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.token").value("test-token"));
    }

    private static Session session(User user) {
        return new Session(user, new Token("test-token", Instant.parse("2030-01-01T00:00:00Z")));
    }

    private static User user(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .username("test-user")
                .firstName("Test")
                .lastName("User")
                .gender(Gender.OTHER)
                .build();
    }
}