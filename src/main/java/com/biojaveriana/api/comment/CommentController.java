package com.biojaveriana.api.comment;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

@Slf4j
@RestController
@RequestMapping("/api/v1/comments")
@Validated
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Endpoints for managing comments on biodiversity observations and posts")
public class CommentController {

    private final CommentService commentService;


    @PostMapping
    @Operation(summary = "Create a comment", description = "Creates a new comment on a post. Supports nested comments via parentId parameter.")
    @ApiResponse(responseCode = "201", description = "Comment created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid comment data")
    public ResponseEntity<CommentResponse> create(
            @Parameter(description = "ID of the post to comment on", required = true) @RequestParam UUID postId,
            @CurrentUser User user,
            @Parameter(description = "Comment creation request body") @RequestBody @Valid CommentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(postId, user, request));
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "Update a comment", description = "Updates an existing comment. Only the author can update their comment.")
    @ApiResponse(responseCode = "200", description = "Comment updated successfully")
    @ApiResponse(responseCode = "404", description = "Comment not found")
    public ResponseEntity<CommentResponse> update(
            @Parameter(description = "ID of the comment to update", required = true) @PathVariable UUID commentId,
            @CurrentUser User user,
            @Parameter(description = "Comment update request body") @RequestBody @Valid CommentUpdateRequest request) {
        return ResponseEntity.ok(commentService.update(commentId, user, request));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a comment", description = "Deletes a comment. Authors can delete their own comments; admins can delete any.")
    @ApiResponse(responseCode = "204", description = "Comment deleted successfully")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the comment to delete", required = true) @PathVariable UUID commentId,
            @CurrentUser User user) {
        commentService.delete(commentId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{commentId}")
    @Operation(summary = "Get a comment by ID", description = "Retrieves a specific comment with its author details")
    @ApiResponse(responseCode = "200", description = "Comment found")
    @ApiResponse(responseCode = "404", description = "Comment not found")
    public ResponseEntity<CommentResponse> getById(
            @Parameter(description = "ID of the comment", required = true) @PathVariable UUID commentId,
            @CurrentUser User user) {
        return ResponseEntity.ok(commentService.getById(commentId, user));
    }


    @GetMapping
    @Operation(summary = "List comments for a post", description = "Retrieves paginated list of top-level comments for a given post")
    @ApiResponse(responseCode = "200", description = "Comments retrieved successfully")
    public ResponseEntity<CommentListResponse> listByPost(
            @Parameter(description = "ID of the post", required = true) @RequestParam UUID postId,
            @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "20")) @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        log.info("Listing comments for post {}", postId);
        return ResponseEntity.ok(commentService.listByPost(postId, page, size));
    }

    @GetMapping("/{parentId}/replies")
    @Operation(summary = "List replies to a comment", description = "Retrieves paginated list of nested replies to a parent comment")
    @ApiResponse(responseCode = "200", description = "Replies retrieved successfully")
    public ResponseEntity<CommentListResponse> listReplies(
            @Parameter(description = "ID of the parent comment", required = true) @PathVariable UUID parentId,
            @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "20")) @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        log.info("Listing replies for parent comment {}", parentId);
        return ResponseEntity.ok(commentService.listReplies(parentId, page, size));
    }


    @PostMapping("/{commentId}/react")
    @Operation(summary = "Toggle comment reaction", description = "Adds or removes an emoji reaction to a comment")
    @ApiResponse(responseCode = "200", description = "Reaction toggled successfully")
    public ResponseEntity<CommentResponse> toggleReaction(
            @Parameter(description = "ID of the comment", required = true) @PathVariable UUID commentId,
            @CurrentUser User user,
            @Parameter(description = "Reaction emoji", required = true) @RequestBody @Valid CommentReactionRequest request) {
        return ResponseEntity.ok(commentService.toggleReaction(commentId, user, request.getEmoji()));
    }

    @DeleteMapping("/{commentId}/react")
    @Operation(summary = "Remove comment reaction", description = "Removes the current user's reaction from a comment")
    @ApiResponse(responseCode = "204", description = "Reaction removed successfully")
    public ResponseEntity<Void> removeReaction(
            @Parameter(description = "ID of the comment", required = true) @PathVariable UUID commentId,
            @CurrentUser User user) {
        commentService.removeReaction(commentId, user);
        return ResponseEntity.noContent().build();
    }

    @HasRole(value = Role.ADMIN)
    @PutMapping("/{commentId}/toggle-hidden")
    @Operation(summary = "Toggle comment visibility (Admin)", description = "Hides or shows a comment. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Comment visibility toggled successfully")
    public ResponseEntity<CommentResponse> toggleHidden(
            @Parameter(description = "ID of the comment", required = true) @PathVariable UUID commentId,
            @CurrentUser User user) {
        return ResponseEntity.ok(commentService.toggleHidden(commentId, user));
    }

    @HasRole(value = Role.ADMIN)
    @PutMapping("/{commentId}/toggle-banned")
    @Operation(summary = "Toggle comment ban status (Admin)", description = "Bans or unbans a comment. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Comment ban status toggled successfully")
    public ResponseEntity<CommentResponse> toggleBanned(
            @Parameter(description = "ID of the comment", required = true) @PathVariable UUID commentId,
            @CurrentUser User user) {
        return ResponseEntity.ok(commentService.toggleBanned(commentId, user));
    }

    @GetMapping("/{postId}/count")
    @Operation(summary = "Count comments for a post", description = "Returns the total number of comments for a given post")
    @ApiResponse(responseCode = "200", description = "Comment count retrieved successfully")
    public ResponseEntity<Long> countByPost(
            @Parameter(description = "ID of the post", required = true) @PathVariable UUID postId) {
        return ResponseEntity.ok(commentService.countByPostId(postId));
    }
}
