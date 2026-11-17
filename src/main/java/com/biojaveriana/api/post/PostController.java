package com.biojaveriana.api.post;

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

import com.biojaveriana.api.post.dto.PostCreateRequest;
import com.biojaveriana.api.post.dto.PostImageUploadPresignRequest;
import com.biojaveriana.api.post.dto.PostListResponse;
import com.biojaveriana.api.post.dto.PostResponse;
import com.biojaveriana.api.post.dto.PostUpdateRequest;
import com.biojaveriana.api.post.dto.PostVoteRequest;
import com.biojaveriana.api.s3.UploadPresign;
import com.biojaveriana.api.security.CurrentUser;
import com.biojaveriana.api.security.HasRole;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.Role;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/posts")
@Validated
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Endpoints for managing posts, votes, and media presign URLs")
public class PostController {

    private final PostService postService;

    @PostMapping("/presign")
    @Operation(summary = "Create presign URL for post image upload", description = "Generates a pre-signed URL for direct S3 upload of post images")
    @ApiResponse(responseCode = "200", description = "Presign URL generated successfully")
    public ResponseEntity<UploadPresign> createPresign(
            @CurrentUser @Parameter(hidden = true) User user,
            @Parameter(description = "Content type of the image", required = true) @RequestBody @Valid PostImageUploadPresignRequest request) {
        return ResponseEntity.ok(postService.createPresign(user, request.getContentType()));
    }

    @PostMapping
    @Operation(summary = "Create a post", description = "Creates a new post for the authenticated user")
    @ApiResponse(responseCode = "201", description = "Post created successfully")
    public ResponseEntity<PostResponse> create(
            @CurrentUser @Parameter(hidden = true) User user,
            @Parameter(description = "Post creation request body", required = true) @RequestBody @Valid PostCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.create(user, request));
    }

    @PutMapping("/{postId}")
    @Operation(summary = "Update a post", description = "Updates a post. Only the author can update their own post")
    @ApiResponse(responseCode = "200", description = "Post updated successfully")
    @ApiResponse(responseCode = "403", description = "User is not the author")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<PostResponse> update(
            @Parameter(description = "ID of the post to update", required = true) @PathVariable UUID postId,
            @CurrentUser @Parameter(hidden = true) User user,
            @Parameter(description = "Post update request body", required = true) @RequestBody @Valid PostUpdateRequest request) {
        return ResponseEntity.ok(postService.update(postId, user, request));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete a post", description = "Deletes a post. Only the author can delete their own post")
    @ApiResponse(responseCode = "204", description = "Post deleted successfully")
    @ApiResponse(responseCode = "403", description = "User is not the author")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the post to delete", required = true) @PathVariable UUID postId,
            @CurrentUser @Parameter(hidden = true) User user) {
        postService.delete(postId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Get post by ID", description = "Retrieves a post by its unique ID")
    @ApiResponse(responseCode = "200", description = "Post found")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<PostResponse> getById(
            @Parameter(description = "ID of the post", required = true) @PathVariable UUID postId,
            @CurrentUser @Parameter(hidden = true) User user) {
        return ResponseEntity.ok(postService.getById(postId, user));
    }

    @GetMapping
    @Operation(summary = "List posts for user", description = "Retrieves a paginated list of posts for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Posts retrieved successfully")
    public ResponseEntity<PostListResponse> list(
            @CurrentUser @Parameter(hidden = true) User user,
            @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "20")) @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        log.info("Listing posts for user {}", user.getId());
        return ResponseEntity.ok(postService.list(user, page, size));
    }

    @GetMapping("/authors/{authorId}")
    @Operation(summary = "List posts by author", description = "Retrieves a paginated list of posts by a specific author")
    @ApiResponse(responseCode = "200", description = "Posts retrieved successfully")
    public ResponseEntity<PostListResponse> listByAuthor(
            @Parameter(description = "ID of the author", required = true) @PathVariable UUID authorId,
            @CurrentUser @Parameter(hidden = true) User currentUser,
            @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "20")) @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        log.info("Listing posts by author {}", authorId);
        return ResponseEntity.ok(postService.listByAuthor(authorId, currentUser, page, size));
    }

    @PostMapping("/{postId}/vote")
    @Operation(summary = "Vote on a post", description = "Adds or updates a vote on a post by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Vote updated successfully")
    public ResponseEntity<PostResponse> vote(
            @Parameter(description = "ID of the post to vote on", required = true) @PathVariable UUID postId,
            @CurrentUser @Parameter(hidden = true) User user,
            @Parameter(description = "Vote request body (upvote or downvote)", required = true) @RequestBody @Valid PostVoteRequest request) {
        return ResponseEntity.ok(postService.vote(postId, user, request));
    }

    @DeleteMapping("/{postId}/vote")
    @Operation(summary = "Remove vote on a post", description = "Removes the authenticated user's vote on a post")
    @ApiResponse(responseCode = "200", description = "Vote removed successfully")
    public ResponseEntity<PostResponse> removeVote(
            @Parameter(description = "ID of the post", required = true) @PathVariable UUID postId,
            @CurrentUser @Parameter(hidden = true) User user) {
        return ResponseEntity.ok(postService.removeVote(postId, user));
    }

    @GetMapping("/admin")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "List all posts (Admin)", description = "Retrieves a paginated list of all posts. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Posts retrieved successfully")
    public ResponseEntity<PostListResponse> listAllForAdmin(
            @Parameter(description = "Page number (0-based)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "0")) @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (max 100)", schema = @io.swagger.v3.oas.annotations.parameters.Schema(defaultValue = "50")) @RequestParam(defaultValue = "50") @Max(value = 100) int size) {
        log.info("Listing all posts for admin");
        return ResponseEntity.ok(postService.listAllForAdmin(page, size));
    }

    @PutMapping("/{postId}/toggle-hidden")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Toggle post hidden status (Admin)", description = "Toggles whether a post is hidden from public view. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Post hidden status toggled successfully")
    public ResponseEntity<PostResponse> toggleHidden(
            @Parameter(description = "ID of the post", required = true) @PathVariable UUID postId,
            @CurrentUser @Parameter(hidden = true) User user) {
        return ResponseEntity.ok(postService.toggleHidden(postId, user));
    }

    @PutMapping("/{postId}/toggle-banned")
    @HasRole(value = Role.ADMIN)
    @Operation(summary = "Toggle post banned status (Admin)", description = "Toggles whether a post is banned. Admin-only operation.")
    @ApiResponse(responseCode = "200", description = "Post banned status toggled successfully")
    public ResponseEntity<PostResponse> toggleBanned(
            @Parameter(description = "ID of the post", required = true) @PathVariable UUID postId,
            @CurrentUser @Parameter(hidden = true) User user) {
        return ResponseEntity.ok(postService.toggleBanned(postId, user));
    }
}

