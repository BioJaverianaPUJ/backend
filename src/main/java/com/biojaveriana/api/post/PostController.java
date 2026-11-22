package com.biojaveriana.api.post;

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

import com.biojaveriana.api.common.dto.ErrorResponse;
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
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Community posts: creation with image uploads, editing, voting and moderation.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/posts")
@Validated
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Community posts with images and optional linked observations. " +
    "Use the presign flow to upload images to object storage first, then reference the returned keys. " +
    "Only the author of a post can edit or delete it; admins can moderate any post.")
public class PostController {
    private final PostService postService;

    @Operation(summary = "Create a presigned image upload URL",
        description = "Returns a short-lived signed URL and object key for uploading a post image " +
            "directly to object storage. Upload the image to `url` with `method` and `contentType`, " +
            "then reference `key` in the post create/update body.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Presigned upload details"),
        @ApiResponse(responseCode = "400", description = "Validation failed (missing content type)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "415", description = "Unsupported image content type"),
    })
    @PostMapping("/presign")
    public ResponseEntity<UploadPresign> createPresign(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody @Valid PostImageUploadPresignRequest request) {
        return ResponseEntity.ok(postService.createPresign(user, request.getContentType()));
    }

    @Operation(summary = "Create a post",
        description = "Publishes a new post. `imageKeys` must reference already-uploaded images " +
            "(via the presign flow); `observationId` optionally links a species observation. " +
            "A user can have only a limited number of active posts.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Post created"),
        @ApiResponse(responseCode = "400", description = "Validation failed or the user's post image quota is exceeded",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Referenced image key or observation not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping
    public ResponseEntity<PostResponse> create(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody @Valid PostCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.create(user, request));
    }

    @Operation(summary = "Update a post",
        description = "Partially updates a post (title, description and/or linked observation). " +
            "Only the author may update their post.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Post updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Not the author of this post"),
        @ApiResponse(responseCode = "404", description = "Post not found"),
    })
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> update(
            @Parameter(description = "ID of the post to update", required = true) @PathVariable UUID postId,
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody @Valid PostUpdateRequest request) {
        return ResponseEntity.ok(postService.update(postId, user, request));
    }

    @Operation(summary = "Delete a post",
        description = "Deletes the post and its attached images. Only the author may delete their post.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Post deleted"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Not the author of this post"),
        @ApiResponse(responseCode = "404", description = "Post not found"),
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID of the post to delete", required = true) @PathVariable UUID postId,
            @Parameter(hidden = true) @CurrentUser User user) {
        postService.delete(postId, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a post by ID",
        description = "Fetches a single post. Hidden posts are only visible to their author or admins.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The post"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "404", description = "Post not found"),
    })
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getById(
            @Parameter(description = "ID of the post to fetch", required = true) @PathVariable UUID postId,
            @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(postService.getById(postId, user));
    }

    @Operation(summary = "List posts",
        description = "Paginated feed of visible posts, newest first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of posts"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
    })
    @GetMapping
    public ResponseEntity<PostListResponse> list(
            @Parameter(hidden = true) @CurrentUser User user,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (1..100)", example = "20")
            @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        log.info("Listing posts for user {}", user.getId());
        return ResponseEntity.ok(postService.list(user, page, size));
    }

    @Operation(summary = "List a user's posts",
        description = "Paginated feed of all visible posts published by the given author.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of posts"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "404", description = "Author not found"),
    })
    @GetMapping("/authors/{authorId}")
    public ResponseEntity<PostListResponse> listByAuthor(
            @Parameter(description = "ID of the author", required = true) @PathVariable UUID authorId,
            @Parameter(hidden = true) @CurrentUser User currentUser,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (1..100)", example = "20")
            @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        log.info("Listing posts by author {}", authorId);
        return ResponseEntity.ok(postService.listByAuthor(authorId, currentUser, page, size));
    }

    @Operation(summary = "Vote on a post",
        description = "Casts or replaces the authenticated user's vote on the post " +
            "(`LIKE` or `DISLIKE`).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The post with updated vote counts"),
        @ApiResponse(responseCode = "400", description = "Validation failed (missing vote type)"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "404", description = "Post not found"),
        @ApiResponse(responseCode = "409", description = "User already voted on this post"),
    })
    @PostMapping("/{postId}/vote")
    public ResponseEntity<PostResponse> vote(
            @Parameter(description = "ID of the post to vote on", required = true) @PathVariable UUID postId,
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody @Valid PostVoteRequest request) {
        return ResponseEntity.ok(postService.vote(postId, user, request));
    }

    @Operation(summary = "Remove my vote from a post",
        description = "Removes the authenticated user's vote from the post.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The post with updated vote counts"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "404", description = "Post not found"),
    })
    @DeleteMapping("/{postId}/vote")
    public ResponseEntity<PostResponse> removeVote(
            @Parameter(description = "ID of the post to un-vote", required = true) @PathVariable UUID postId,
            @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(postService.removeVote(postId, user));
    }

    @Operation(summary = "List all posts (admin)",
        description = "Moderation view: every post, including hidden and banned ones. " +
            "Requires the ADMIN role (SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of all posts"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
    })
    @GetMapping("/admin")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<PostListResponse> listAllForAdmin(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @Parameter(description = "Page size (1..100)", example = "50")
            @RequestParam(defaultValue = "50") @Max(value = 100) int size) {
        log.info("Listing all posts for admin");
        return ResponseEntity.ok(postService.listAllForAdmin(page, size));
    }

    @Operation(summary = "Toggle post visibility (admin)",
        description = "Hides or un-hides a post for regular users. Requires the ADMIN role " +
            "(SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The post with its new hidden state"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Post not found"),
    })
    @PutMapping("/{postId}/toggle-hidden")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<PostResponse> toggleHidden(
            @Parameter(description = "ID of the post to toggle", required = true) @PathVariable UUID postId,
            @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(postService.toggleHidden(postId, user));
    }

    @Operation(summary = "Toggle post banned state (admin)",
        description = "Bans or un-bans a post, permanently suppressing it. Requires the ADMIN role " +
            "(SUPERADMIN bypasses the check).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The post with its new banned state"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "404", description = "Post not found"),
    })
    @PutMapping("/{postId}/toggle-banned")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<PostResponse> toggleBanned(
            @Parameter(description = "ID of the post to toggle", required = true) @PathVariable UUID postId,
            @Parameter(hidden = true) @CurrentUser User user) {
        return ResponseEntity.ok(postService.toggleBanned(postId, user));
    }
}

