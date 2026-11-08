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
public class PostController {

    private final PostService postService;

    @PostMapping("/presign")
    public ResponseEntity<UploadPresign> createPresign(
            @CurrentUser User user,
            @RequestBody @Valid PostImageUploadPresignRequest request) {
        return ResponseEntity.ok(postService.createPresign(user, request.getContentType()));
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(
            @CurrentUser User user,
            @RequestBody @Valid PostCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.create(user, request));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> update(
            @PathVariable UUID postId,
            @CurrentUser User user,
            @RequestBody @Valid PostUpdateRequest request) {
        return ResponseEntity.ok(postService.update(postId, user, request));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID postId,
            @CurrentUser User user) {
        postService.delete(postId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getById(
            @PathVariable UUID postId,
            @CurrentUser User user) {
        return ResponseEntity.ok(postService.getById(postId, user));
    }

    @GetMapping
    public ResponseEntity<PostListResponse> list(
            @CurrentUser User user,
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        log.info("Listing posts for user {}", user.getId());
        return ResponseEntity.ok(postService.list(user, page, size));
    }

    @GetMapping("/authors/{authorId}")
    public ResponseEntity<PostListResponse> listByAuthor(
            @PathVariable UUID authorId,
            @CurrentUser User currentUser,
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @RequestParam(defaultValue = "20") @Max(value = 100) int size) {
        log.info("Listing posts by author {}", authorId);
        return ResponseEntity.ok(postService.listByAuthor(authorId, currentUser, page, size));
    }

    @PostMapping("/{postId}/vote")
    public ResponseEntity<PostResponse> vote(
            @PathVariable UUID postId,
            @CurrentUser User user,
            @RequestBody @Valid PostVoteRequest request) {
        return ResponseEntity.ok(postService.vote(postId, user, request));
    }

    @DeleteMapping("/{postId}/vote")
    public ResponseEntity<PostResponse> removeVote(
            @PathVariable UUID postId,
            @CurrentUser User user) {
        return ResponseEntity.ok(postService.removeVote(postId, user));
    }

    @GetMapping("/admin")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<PostListResponse> listAllForAdmin(
            @RequestParam(defaultValue = "0") @Min(value = 0) int page,
            @RequestParam(defaultValue = "50") @Max(value = 100) int size) {
        log.info("Listing all posts for admin");
        return ResponseEntity.ok(postService.listAllForAdmin(page, size));
    }

    @PutMapping("/{postId}/toggle-hidden")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<PostResponse> toggleHidden(
            @PathVariable UUID postId,
            @CurrentUser User user) {
        return ResponseEntity.ok(postService.toggleHidden(postId, user));
    }

    @PutMapping("/{postId}/toggle-banned")
    @HasRole(value = Role.ADMIN)
    public ResponseEntity<PostResponse> toggleBanned(
            @PathVariable UUID postId,
            @CurrentUser User user) {
        return ResponseEntity.ok(postService.toggleBanned(postId, user));
    }
}

