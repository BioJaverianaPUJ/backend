package com.biojaveriana.api.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.biojaveriana.api.comment.dto.CommentListResponse;
import com.biojaveriana.api.comment.dto.CommentResponse;
import com.biojaveriana.api.comment.dto.CommentSummaryResponse;
import com.biojaveriana.api.comment.dto.ReactionSummaryResponse;
import com.biojaveriana.api.comment.dto.UserSummaryResponse;
import com.biojaveriana.api.user.BanService;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserService;

@WebMvcTest(CommentController.class)
@Import(SecuritySliceConfiguration.class)
class CommentControllerTest {

    @MockitoBean
    private CommentService commentService;

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

    private static UserSummaryResponse author() {
        return new UserSummaryResponse(UUID.randomUUID(), "jane", "Jane", "Doe", null);
    }

    private CommentResponse commentResponse(UUID id) {
        return new CommentResponse(id, author(), UUID.randomUUID(), null, "Nice find!", 1,
            List.of(new ReactionSummaryResponse(ReactionType.LOVE, 1, List.of(author()))),
            false, null, null);
    }

    private CommentListResponse commentList(UUID postId) {
        CommentSummaryResponse summary =
            new CommentSummaryResponse(UUID.randomUUID(), author(), postId, null, "Nice find!", 0, false, null);
        return new CommentListResponse(List.of(summary), new Pagination(0, 20, 1, 1, false, false));
    }

    @Test
    @DisplayName("POST /comments creates a comment on the given post")
    void createComment() throws Exception {
        UUID postId = UUID.randomUUID();
        authenticate(user);
        UUID commentId = UUID.randomUUID();
        when(commentService.create(eq(postId), eq(user), any())).thenReturn(commentResponse(commentId));

        mockMvc.perform(post("/api/v1/comments")
                .param("postId", postId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Nice find!\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.content").value("Nice find!"))
            .andExpect(jsonPath("$.author.username").value("jane"));
    }

    @Test
    @DisplayName("POST /comments returns 400 for blank content")
    void createCommentValidationFails() throws Exception {
        authenticate(user);

        mockMvc.perform(post("/api/v1/comments")
                .param("postId", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"  \"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /comments/{id} updates the comment")
    void updateComment() throws Exception {
        UUID commentId = UUID.randomUUID();
        authenticate(user);
        when(commentService.update(eq(commentId), eq(user), any())).thenReturn(commentResponse(commentId));

        mockMvc.perform(put("/api/v1/comments/" + commentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Updated\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Nice find!"));
    }

    @Test
    @DisplayName("DELETE /comments/{id} deletes the comment")
    void deleteComment() throws Exception {
        UUID commentId = UUID.randomUUID();
        authenticate(user);

        mockMvc.perform(delete("/api/v1/comments/" + commentId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /comments/{id} returns the comment")
    void getCommentById() throws Exception {
        UUID commentId = UUID.randomUUID();
        authenticate(user);
        when(commentService.getById(eq(commentId), eq(user))).thenReturn(commentResponse(commentId));

        mockMvc.perform(get("/api/v1/comments/" + commentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Nice find!"))
            .andExpect(jsonPath("$.totalReactions").value(1));
    }

    @Test
    @DisplayName("GET /comments lists the top-level comments of a post")
    void listByPost() throws Exception {
        UUID postId = UUID.randomUUID();
        when(commentService.listByPost(eq(postId), eq(0), eq(20))).thenReturn(commentList(postId));

        mockMvc.perform(get("/api/v1/comments").param("postId", postId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].content").value("Nice find!"))
            .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /comments/{parentId}/replies lists the replies of a comment")
    void listReplies() throws Exception {
        UUID parentId = UUID.randomUUID();
        when(commentService.listReplies(eq(parentId), eq(0), eq(20))).thenReturn(commentList(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/comments/" + parentId + "/replies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].content").value("Nice find!"))
            .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /comments/{id}/react records a reaction")
    void reactToComment() throws Exception {
        UUID commentId = UUID.randomUUID();
        authenticate(user);
        when(commentService.toggleReaction(eq(commentId), eq(user), eq(ReactionType.LOVE)))
            .thenReturn(commentResponse(commentId));

        mockMvc.perform(post("/api/v1/comments/" + commentId + "/react")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emoji\": \"LOVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalReactions").value(1));
    }

    @Test
    @DisplayName("DELETE /comments/{id}/react removes my reaction")
    void removeReaction() throws Exception {
        UUID commentId = UUID.randomUUID();
        authenticate(user);

        mockMvc.perform(delete("/api/v1/comments/" + commentId + "/react"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /comments/{id}/toggle-hidden toggles the hidden state for admins")
    void toggleHidden() throws Exception {
        UUID commentId = UUID.randomUUID();
        authenticate(admin);
        when(commentService.toggleHidden(eq(commentId), eq(admin))).thenReturn(commentResponse(commentId));

        mockMvc.perform(put("/api/v1/comments/" + commentId + "/toggle-hidden"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Nice find!"));
    }

    @Test
    @DisplayName("PUT /comments/{id}/toggle-banned toggles the banned state for admins")
    void toggleBanned() throws Exception {
        UUID commentId = UUID.randomUUID();
        authenticate(admin);
        when(commentService.toggleBanned(eq(commentId), eq(admin))).thenReturn(commentResponse(commentId));

        mockMvc.perform(put("/api/v1/comments/" + commentId + "/toggle-banned"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Nice find!"));
    }

    @Test
    @DisplayName("GET /comments/{postId}/count returns the comment count as a number")
    void countByPost() throws Exception {
        UUID postId = UUID.randomUUID();
        when(commentService.countByPostId(postId)).thenReturn(42L);

        String body = mockMvc.perform(get("/api/v1/comments/" + postId + "/count"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertEquals("42", body);
    }

}
