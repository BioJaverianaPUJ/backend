package com.biojaveriana.api.post;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.post.dto.PostListResponse;
import com.biojaveriana.api.post.dto.PostResponse;
import com.biojaveriana.api.s3.UploadPresign;
import com.biojaveriana.api.user.BanService;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;

@WebMvcTest(PostController.class)
@Import(SecuritySliceConfiguration.class)
class PostControllerTest {

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private UserService userService;

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
        user = User.builder().id(userId).username("jane").role(Role.USER).build();
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

    private PostResponse postResponse(UUID id) {
        return new PostResponse(id, null, "Title", "Description", null, List.of(), 0, 0, null, null, false, false,
            Instant.now(), Instant.now());
    }

    private PostListResponse postList() {
        return new PostListResponse(List.of(), new Pagination(0, 20, 0, 0, true, true));
    }

    @Test
    @DisplayName("POST /posts/presign returns a presigned upload URL")
    void createPresign() throws Exception {
        authenticate(user);
        UploadPresign presign = new UploadPresign("posts/1.jpg", "https://s3.example.com/signed", "PUT", "image/jpeg",
            Instant.now().plusSeconds(300));
        when(postService.createPresign(eq(user), eq("image/jpeg"))).thenReturn(presign);

        mockMvc.perform(post("/api/v1/posts/presign")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"contentType": "image/jpeg"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("posts/1.jpg"));
    }

    @Test
    @DisplayName("POST /posts creates a post")
    void createPost() throws Exception {
        authenticate(user);
        when(postService.create(eq(user), any())).thenReturn(postResponse(UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Look at this", "description": "Found a cool tree"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    @DisplayName("POST /posts returns 400 without a title")
    void createPostValidationFails() throws Exception {
        authenticate(user);

        mockMvc.perform(post("/api/v1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description": "no title"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /posts/{id} returns the post")
    void getPost() throws Exception {
        authenticate(user);
        UUID id = UUID.randomUUID();
        when(postService.getById(id, user)).thenReturn(postResponse(id));

        mockMvc.perform(get("/api/v1/posts/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("Description"));
    }

    @Test
    @DisplayName("GET /posts lists posts and rejects invalid pagination")
    void listPosts() throws Exception {
        authenticate(user);
        when(postService.list(eq(user), eq(0), eq(20))).thenReturn(postList());

        mockMvc.perform(get("/api/v1/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());

        mockMvc.perform(get("/api/v1/posts").param("page", "-1"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /posts/authors/{id} lists an author's posts")
    void listByAuthor() throws Exception {
        authenticate(user);
        UUID authorId = UUID.randomUUID();
        when(postService.listByAuthor(authorId, user, 0, 20)).thenReturn(postList());

        mockMvc.perform(get("/api/v1/posts/authors/" + authorId))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /posts/{id}/vote casts a vote")
    void vote() throws Exception {
        authenticate(user);
        UUID id = UUID.randomUUID();
        when(postService.vote(eq(id), eq(user), any())).thenReturn(postResponse(id));

        mockMvc.perform(post("/api/v1/posts/" + id + "/vote")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"voteType": "LIKE"}
                    """))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /posts/{id}/vote returns 400 without a vote type")
    void voteValidationFails() throws Exception {
        authenticate(user);
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/posts/" + id + "/vote")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /posts/{id}/vote removes the vote")
    void removeVote() throws Exception {
        authenticate(user);
        UUID id = UUID.randomUUID();
        when(postService.removeVote(id, user)).thenReturn(postResponse(id));

        mockMvc.perform(delete("/api/v1/posts/" + id + "/vote"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /posts/{id}/toggle-hidden is admin-only")
    void toggleHiddenForbiddenForUser() throws Exception {
        authenticate(user);
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/posts/" + id + "/toggle-hidden"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /posts/{id}/toggle-hidden toggles for admins")
    void toggleHiddenAsAdmin() throws Exception {
        authenticate(admin);
        UUID id = UUID.randomUUID();
        when(postService.toggleHidden(id, admin)).thenReturn(postResponse(id));

        mockMvc.perform(put("/api/v1/posts/" + id + "/toggle-hidden"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /posts/admin is admin-only and returns the moderation feed")
    void adminList() throws Exception {
        authenticate(user);

        mockMvc.perform(get("/api/v1/posts/admin"))
            .andExpect(status().isForbidden());

        authenticate(admin);
        when(postService.listAllForAdmin(0, 50)).thenReturn(postList());

        mockMvc.perform(get("/api/v1/posts/admin"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /posts/{id} removes the post")
    void deletePost() throws Exception {
        authenticate(user);
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/posts/" + id))
            .andExpect(status().isNoContent());
    }
}
