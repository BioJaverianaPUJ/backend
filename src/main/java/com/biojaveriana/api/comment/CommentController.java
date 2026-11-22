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
import com.biojaveriana.api.common.dto.ErrorResponse;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

/**
 * Comments on posts, replies, reactions and comment moderation.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/comments")
@Validated
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Comments on posts: creating, editing, replying, reacting " +
    "and (for admins) hiding/banning comments. Only the author of a comment can edit or delete it.")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "Create a comment",
        description = "Adds a top-level comment to the given post. Set `parentId` in the body " +
            "to reply to an existing comment instead.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Comment created"),
        @ApiResponse(responseCode = "400", description = "Validation failed (blank or too long content)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "The post is hidden or banned",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Post (or parent comment) not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @Parameter(description = "ID of the post to comment on", required = true) @RequestParam UUID postId,
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody @Valid CommentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(postId, user, request));
    }

    @Operation(summary = "Update a comment",
        description = "Replaces the comment's content. Only the author (or an admin) may update it.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comment updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed (blank or too long content)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not the author of this comment",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Comment not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> update(
            @Parameter(description = "ID of the comment to update", required = true) @PathVariable UUID commentId,
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody @Valid CommentUpdateRequest request) {
        return ResponseEntity.ok(commentService.update(commentId, user, request));
    }

    @Operation(summary = "Delete a comment",
        description = "Deletes the comment. Only the author (or an admin) may delete it.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Comment deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not the author of this comment",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Comment not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the comment to delete", required = true) @PathVariable UUID commentId,
            @Parameter(hidden = true) @CurrentUser User user) {
        commentService.delete(commentId, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a comment by ID",
        description = "Fetches a single comment. Hidden comments are only visible to their author or admins.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The comment"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Comment not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponse> getById(
            @Parameter(description = "ID of the comment to fetch", required = true) @PathVariable UUID commentId,
            @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(commentService.getById(commentId, user));
    }


    @Operation(summary = "List a post's top-level comments",
        description = "Paginated list of the top-level comments of a post (replies are fetched " +
            "separately through the replies endpoint).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of comments"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "404", description = "Post not found"),
    })
    @GetMapping
    public ResponseEntity<CommentListResponse> listByPost(
            @Parameter(description = "ID of the post", required = true) @RequestParam UUID postId,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (1..100)", example = "20")
            @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        log.info("Listing comments for post {}", postId);
        return ResponseEntity.ok(commentService.listByPost(postId, page, size));
    }

    @Operation(summary = "List replies to a comment",
        description = "Paginated list of the direct replies of the given parent comment.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of replies"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "404", description = "Parent comment not found"),
    })
    @GetMapping("/{parentId}/replies")
    public ResponseEntity<CommentListResponse> listReplies(
            @Parameter(description = "ID of the parent comment", required = true) @PathVariable UUID parentId,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (1..100)", example = "20")
            @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        log.info("Listing replies for parent comment {}", parentId);
        return ResponseEntity.ok(commentService.listReplies(parentId, page, size));
    }

    @Operation(summary = "React to a comment",
        description = "Adds (or re-uses) the authenticated user's reaction on the comment. " +
            "Each user can react once per comment.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The comment with updated reaction counts"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "404", description = "Comment not found"),
        @ApiResponse(responseCode = "409", description = "User already reacted to this comment"),
    })
    @PostMapping("/{commentId}/react")
    public ResponseEntity<CommentResponse> toggleReaction(
            @Parameter(description = "ID of the comment to react to", required = true) @PathVariable UUID commentId,
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody @Valid CommentReactionRequest request) {
        return ResponseEntity.ok(commentService.toggleReaction(commentId, user, request.getEmoji()));
    }

    @Operation(summary = "Remove my reaction from a comment",
        description = "Removes the authenticated user's reaction from the comment.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Reaction removed"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "404", description = "Comment not found"),
    })
    @DeleteMapping("/{commentId}/react")
    public ResponseEntity<Void> removeReaction(
            @Parameter(description = "ID of the comment to un-react", required = true) @PathVariable UUID commentId,
            @Parameter(hidden = true) @CurrentUser User user) {
        commentService.removeReaction(commentId, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Toggle comment visibility (admin)",
        description = "Hides or un-hides a comment for regular users. Requires the ADMIN role " +
            "(SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The comment with its new hidden state"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Comment not found"),
    })
    @HasRole(value = Role.ADMIN)
    @PutMapping("/{commentId}/toggle-hidden")
    public ResponseEntity<CommentResponse> toggleHidden(
            @Parameter(description = "ID of the comment to toggle", required = true) @PathVariable UUID commentId,
            @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(commentService.toggleHidden(commentId, user));
    }

    @Operation(summary = "Toggle comment banned state (admin)",
        description = "Bans or un-bans a comment, permanently suppressing it. Requires the ADMIN role " +
            "(SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The comment with its new banned state"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Comment not found"),
    })
    @HasRole(value = Role.ADMIN)
    @PutMapping("/{commentId}/toggle-banned")
    public ResponseEntity<CommentResponse> toggleBanned(
            @Parameter(description = "ID of the comment to toggle", required = true) @PathVariable UUID commentId,
            @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(commentService.toggleBanned(commentId, user));
    }

    @Operation(summary = "Count a post's comments",
        description = "Returns the total number of comments of a post as a plain JSON number.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comment count"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping("/{postId}/count")
    public ResponseEntity<Long> countByPost(
            @Parameter(description = "ID of the post", required = true) @PathVariable UUID postId) {
        return ResponseEntity.ok(commentService.countByPostId(postId));
    }
}
