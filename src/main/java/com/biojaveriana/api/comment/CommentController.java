package com.biojaveriana.api.comment;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.biojaveriana.api.comment.dto.CommentCreateRequest;
import com.biojaveriana.api.comment.dto.CommentListResponse;
import com.biojaveriana.api.comment.dto.CommentReactionRequest;
import com.biojaveriana.api.comment.dto.CommentResponse;
import com.biojaveriana.api.comment.dto.CommentUpdateRequest;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.Role;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;


    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @RequestParam UUID postId,
            @CurrentUser User user,
            @RequestBody @Valid CommentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(postId, user, request));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> update(
            @PathVariable UUID commentId,
            @CurrentUser User user,
            @RequestBody @Valid CommentUpdateRequest request) {
        return ResponseEntity.ok(commentService.update(commentId, user, request));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID commentId,
            @CurrentUser User user) {
        commentService.delete(commentId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponse> getById(
            @PathVariable UUID commentId,
            @CurrentUser User user) {
        return ResponseEntity.ok(commentService.getById(commentId, user));
    }


    @GetMapping
    public ResponseEntity<CommentListResponse> listByPost(
            @RequestParam UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(commentService.listByPost(postId, page, size));
    }

    @GetMapping("/{parentId}/replies")
    public ResponseEntity<CommentListResponse> listReplies(
            @PathVariable UUID parentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(commentService.listReplies(parentId, page, size));
    }


    @PostMapping("/{commentId}/react")
    public ResponseEntity<CommentResponse> toggleReaction(
            @PathVariable UUID commentId,
            @CurrentUser User user,
            @RequestBody @Valid CommentReactionRequest request) {
        return ResponseEntity.ok(commentService.toggleReaction(commentId, user, request.getEmoji()));
    }

    @DeleteMapping("/{commentId}/react")
    public ResponseEntity<Void> removeReaction(
            @PathVariable UUID commentId,
            @CurrentUser User user) {
        commentService.removeReaction(commentId, user);
        return ResponseEntity.noContent().build();
    }

    @HasRole(value = Role.ADMIN)
    @PutMapping("/{commentId}/toggle-hidden")
    public ResponseEntity<CommentResponse> toggleHidden(
            @PathVariable UUID commentId,
            @CurrentUser User user) {
        return ResponseEntity.ok(commentService.toggleHidden(commentId, user));
    }

    @HasRole(value = Role.ADMIN)
    @PutMapping("/{commentId}/toggle-banned")
    public ResponseEntity<CommentResponse> toggleBanned(
            @PathVariable UUID commentId,
            @CurrentUser User user) {
        return ResponseEntity.ok(commentService.toggleBanned(commentId, user));
    }

    @GetMapping("/{postId}/count")
    public ResponseEntity<Long> countByPost(
            @PathVariable UUID postId) {
        return ResponseEntity.ok(commentService.countByPostId(postId));
    }
}
