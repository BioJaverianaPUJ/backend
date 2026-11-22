package com.biojaveriana.api.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpMethod;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.biojaveriana.api.SecuritySliceConfiguration;
import com.biojaveriana.api.s3.ObjectService;

@WebMvcTest(UserController.class)
@Import(SecuritySliceConfiguration.class)
class UserControllerTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDeletionService userDeletionService;

    @MockitoBean
    private ObjectService objectService;

    @MockitoBean
    private BanService banService;

    @Autowired
    private MockMvc mockMvc;

    private User user;
    private User admin;
    private UUID userId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).username("jane").firstName("Jane").lastName("Doe").role(Role.USER).build();
        adminId = UUID.randomUUID();
        admin = User.builder().id(adminId).username("admin").role(Role.ADMIN).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(User caller) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(caller.getId().toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(jwt, "N/A", List.of()));
        when(userService.findById(caller.getId())).thenReturn(caller);
    }

    @Test
    @DisplayName("GET /users/me returns the authenticated user's profile")
    void getCurrentUser() throws Exception {
        authenticate(user);

        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("jane"))
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("GET /users/{id} returns a user by ID")
    void getUserById() throws Exception {
        authenticate(user);
        when(userService.findById(userId)).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/" + userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("jane"));
    }

    @Test
    @DisplayName("GET /users/@{username} returns a user by username")
    void getUserByAtUsername() throws Exception {
        authenticate(user);
        when(userService.findByUsername("jane")).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/@jane"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("jane"));
    }

    @Test
    @DisplayName("GET /users lists users paginated")
    void findAll() throws Exception {
        authenticate(user);
        when(userService.findAll(any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].username").value("jane"))
            .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    @DisplayName("PATCH /users/{id} updates a user for admins")
    void updateUserById() throws Exception {
        authenticate(admin);
        when(userService.updateById(eq(userId), any())).thenReturn(user);

        mockMvc.perform(patch("/api/v1/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\": \"Janet\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("jane"));
    }

    @Test
    @DisplayName("PATCH /users/me updates the authenticated user's profile")
    void updateMe() throws Exception {
        authenticate(user);
        when(userService.updateMe(eq(user), any())).thenReturn(user);

        mockMvc.perform(patch("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\": \"Janet\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("jane"));
    }

    @Test
    @DisplayName("PATCH /users/me/password changes the password")
    void changePassword() throws Exception {
        authenticate(user);
        when(userService.changePassword(eq(user), any())).thenReturn(user);

        mockMvc.perform(patch("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\": \"old-password-1\", \"newPassword\": \"new-password-1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("jane"));
    }

    @Test
    @DisplayName("DELETE /users/me deletes the authenticated user's account")
    void deleteMyAccount() throws Exception {
        authenticate(user);

        mockMvc.perform(delete("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\": \"correct-password-1\"}"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /users/me/media clears the media without an existing key")
    void deleteMedia() throws Exception {
        authenticate(user);

        mockMvc.perform(delete("/api/v1/users/me/media").param("type", "banner"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("jane"));
    }

    @Test
    @DisplayName("PATCH /users/me/media uploads a profile picture")
    void uploadMedia() throws Exception {
        authenticate(user);
        MockMultipartFile file =
            new MockMultipartFile("file", "picture.png", "image/png", new byte[] {1, 2, 3});

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/users/me/media")
                .param("type", "profile-picture")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("jane"));
    }

    @Test
    @DisplayName("DELETE /users/{id} deletes another user for admins")
    void deleteUserById() throws Exception {
        authenticate(admin);
        when(userService.findById(userId)).thenReturn(user);

        mockMvc.perform(delete("/api/v1/users/" + userId))
            .andExpect(status().isNoContent());
    }

}
