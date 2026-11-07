package com.biojaveriana.api.post;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.post.dto.PostCreateRequest;
import com.biojaveriana.api.post.dto.PostImageResponse;
import com.biojaveriana.api.post.dto.PostListResponse;
import com.biojaveriana.api.post.dto.PostResponse;
import com.biojaveriana.api.post.dto.PostSummaryResponse;
import com.biojaveriana.api.post.dto.PostUpdateRequest;
import com.biojaveriana.api.post.dto.PostVoteRequest;
import com.biojaveriana.api.post.dto.UserSummaryResponse;
import com.biojaveriana.api.post.exceptions.PostForbiddenException;
import com.biojaveriana.api.post.exceptions.PostImageLimitException;
import com.biojaveriana.api.post.exceptions.PostNotFoundException;
import com.biojaveriana.api.s3.S3UrlGenerator;
import com.biojaveriana.api.s3.UploadPresign;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;
import com.biojaveriana.api.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
    private static final int MAX_IMAGES = 10;
    private static final String POSTS_PREFIX = "posts/";

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostVoteRepository postVoteRepository;
    private final UserRepository userRepository;
    private final S3UrlGenerator s3UrlGenerator;

    @Transactional
    public UploadPresign createPresign(User user, String contentType) {
        String key = POSTS_PREFIX + user.getId() + "/" + UUID.randomUUID() + "." + getFileExtension(contentType);
        return new UploadPresign(key, "", "PUT", contentType, Instant.now().plus(Duration.ofMinutes(5)));
    }

    @Transactional
    public PostResponse create(User user, PostCreateRequest request) {
        if (request.getImageKeys() != null && request.getImageKeys().length > MAX_IMAGES) {
            throw new PostImageLimitException(MAX_IMAGES);
        }

        var post = Post.builder()
            .author(user)
            .title(request.getTitle())
            .description(request.getDescription())
            .observation(request.getObservation())
            .totalLikes(0)
            .totalDislikes(0)
            .hidden(false)
            .banned(false)
            .build();

        post = postRepository.save(post);

        if (request.getImageKeys() != null) {
            List<PostImage> images = new ArrayList<>();
            for (int i = 0; i < request.getImageKeys().length; i++) {
                var image = PostImage.builder()
                    .post(post)
                    .s3Key(request.getImageKeys()[i])
                    .orderIndex(i)
                    .build();
                images.add(image);
            }
            postImageRepository.saveAll(images);
        }

        log.info("Post created id={} by user id={}", post.getId(), user.getId());
        return toResponse(post, null);
    }

    @Transactional
    public PostResponse update(UUID postId, User user, PostUpdateRequest request) {
        var post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.getAuthor().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new PostForbiddenException();
        }

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            post.setDescription(request.getDescription());
        }
        if (request.getObservation() != null) {
            post.setObservation(request.getObservation());
        }

        post = postRepository.save(post);
        log.info("Post updated id={} by user id={}", postId, user.getId());
        return toResponse(post, null);
    }

    @Transactional
    public void delete(UUID postId, User user) {
        var post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.getAuthor().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new PostForbiddenException();
        }

        postImageRepository.deleteByPostId(postId);
        postRepository.delete(post);
        log.info("Post deleted id={} by user id={}", postId, user.getId());
    }

    @Transactional(readOnly = true)
    public PostResponse getById(UUID postId, User user) {
        var post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));

        if (post.getBanned()) {
            if (user.getRole() != Role.ADMIN && !post.getAuthor().getId().equals(user.getId())) {
                throw new PostNotFoundException(postId);
            }
        }

        var existingVote = postVoteRepository.findByPostIdAndVoterId(postId, user.getId()).orElse(null);
        return toResponse(post, existingVote);
    }

    @Transactional(readOnly = true)
    public PostListResponse list(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> postPage;

        if (user.getRole() == Role.ADMIN) {
            postPage = postRepository.findAll(pageable);
        } else {
            postPage = postRepository.findByHiddenFalseAndBannedFalseOrderByCreatedAtDesc(pageable);
        }

        return toListResponse(postPage);
    }

    @Transactional(readOnly = true)
    public PostListResponse listByAuthor(UUID authorId, User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> postPage;

        if (currentUser.getId().equals(authorId)) {
            postPage = postRepository.findByAuthorIdAndHiddenFalseAndBannedFalseOrderByCreatedAtDesc(authorId, pageable);
        } else if (currentUser.getRole() == Role.ADMIN) {
            postPage = postRepository.findByAuthorOrderByCreatedAtDesc(
                userRepository.findById(authorId).orElseThrow(() -> new com.biojaveriana.api.user.exceptions.UserNotFoundException(authorId)),
                pageable
            );
        } else {
            postPage = postRepository.findByAuthorIdAndHiddenFalseAndBannedFalseOrderByCreatedAtDesc(authorId, pageable);
        }

        return toListResponse(postPage);
    }

    @Transactional(readOnly = true)
    public PostListResponse listAllForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> postPage = postRepository.findAll(pageable);
        return toListResponse(postPage);
    }

    @Transactional
    public PostResponse vote(UUID postId, User user, PostVoteRequest request) {
        var post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));

        var existingVote = postVoteRepository.findByPostIdAndVoterId(postId, user.getId()).orElse(null);
        VoteType newVoteType = request.getVoteType();

        if (existingVote != null) {
            if (existingVote.getVoteType() == newVoteType) {
                postVoteRepository.delete(existingVote);
                if (newVoteType == VoteType.LIKE) {
                    post.setTotalLikes(Math.max(0, post.getTotalLikes() - 1));
                } else {
                    post.setTotalDislikes(Math.max(0, post.getTotalDislikes() - 1));
                }
                post = postRepository.save(post);
                log.info("Vote removed from post id={} by user id={}", postId, user.getId());
            } else {
                existingVote.setVoteType(newVoteType);
                postVoteRepository.save(existingVote);
                if (newVoteType == VoteType.LIKE) {
                    post.setTotalLikes(post.getTotalLikes() + 1);
                    post.setTotalDislikes(Math.max(0, post.getTotalDislikes() - 1));
                } else {
                    post.setTotalDislikes(post.getTotalDislikes() + 1);
                    post.setTotalLikes(Math.max(0, post.getTotalLikes() - 1));
                }
                post = postRepository.save(post);
                log.info("Vote toggled on post id={} by user id={}", postId, user.getId());
            }
        } else {
            var vote = PostVote.builder()
                .post(post)
                .voter(user)
                .voteType(newVoteType)
                .build();
            postVoteRepository.save(vote);
            if (newVoteType == VoteType.LIKE) {
                post.setTotalLikes(post.getTotalLikes() + 1);
            } else {
                post.setTotalDislikes(post.getTotalDislikes() + 1);
            }
            post = postRepository.save(post);
            log.info("Vote added to post id={} by user id={}", postId, user.getId());
        }

        return toResponse(post, existingVote);
    }

    @Transactional
    public PostResponse removeVote(UUID postId, User user) {
        var post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));

        var existingVote = postVoteRepository.findByPostIdAndVoterId(postId, user.getId()).orElse(null);
        if (existingVote == null) {
            throw new com.biojaveriana.api.post.exceptions.PostAlreadyVotedException();
        }

        if (existingVote.getVoteType() == VoteType.LIKE) {
            post.setTotalLikes(Math.max(0, post.getTotalLikes() - 1));
        } else {
            post.setTotalDislikes(Math.max(0, post.getTotalDislikes() - 1));
        }

        postVoteRepository.delete(existingVote);
        post = postRepository.save(post);
        log.info("Vote removed from post id={} by user id={}", postId, user.getId());

        return toResponse(post, null);
    }

    @Transactional
    public PostResponse toggleHidden(UUID postId, User user) {
        if (user.getRole() != Role.ADMIN) {
            throw new PostForbiddenException();
        }
        var post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));
        post.setHidden(!post.getHidden());
        post = postRepository.save(post);
        log.info("Post {} hidden status toggled to {} by admin user id={}", postId, post.getHidden(), user.getId());
        return toResponse(post, null);
    }

    @Transactional
    public PostResponse toggleBanned(UUID postId, User user) {
        if (user.getRole() != Role.ADMIN) {
            throw new PostForbiddenException();
        }
        var post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));
        post.setBanned(!post.getBanned());
        post = postRepository.save(post);
        log.info("Post {} banned status toggled to {} by admin user id={}", postId, post.getBanned(), user.getId());
        return toResponse(post, null);
    }

    private PostResponse toResponse(Post post, PostVote existingVote) {
        List<PostImageResponse> images = post.getImages() != null
            ? post.getImages().stream()
                .sorted(Comparator.comparingInt(image -> image.getOrderIndex()))
                .map(img -> new PostImageResponse(
                    img.getId(),
                    img.getS3Key(),
                    s3UrlGenerator.generateUrl(img.getS3Key()),
                    img.getOrderIndex()))
                .toList()
            : List.of();

        Boolean userVoted = existingVote != null;
        VoteType userVoteType = existingVote != null ? existingVote.getVoteType() : null;

        return new PostResponse(
            post.getId(),
            new UserSummaryResponse(
                post.getAuthor().getId(),
                post.getAuthor().getUsername(),
                post.getAuthor().getFirstName(),
                post.getAuthor().getLastName(),
                post.getAuthor().getProfilePictureKey() != null
                    ? s3UrlGenerator.generateUrl(post.getAuthor().getProfilePictureKey())
                    : null),
            post.getTitle(),
            post.getDescription(),
            post.getObservation(),
            images,
            post.getTotalLikes(),
            post.getTotalDislikes(),
            userVoted,
            userVoteType,
            post.getBanned(),
            post.getHidden(),
            post.getCreatedAt(),
            post.getUpdatedAt()
        );
    }

    private PostListResponse toListResponse(Page<Post> postPage) {
        List<PostSummaryResponse> content = postPage.getContent().stream()
            .map(post -> {
                String firstImageUrl = null;
                if (post.getImages() != null && !post.getImages().isEmpty()) {
                    firstImageUrl = s3UrlGenerator.generateUrl(post.getImages().get(0).getS3Key());
                }
                return new PostSummaryResponse(
                    post.getId(),
                    new UserSummaryResponse(
                        post.getAuthor().getId(),
                        post.getAuthor().getUsername(),
                        post.getAuthor().getFirstName(),
                        post.getAuthor().getLastName(),
                        post.getAuthor().getProfilePictureKey() != null
                            ? s3UrlGenerator.generateUrl(post.getAuthor().getProfilePictureKey())
                            : null),
                    post.getTitle(),
                    firstImageUrl,
                    post.getTotalLikes(),
                    post.getTotalDislikes(),
                    post.getBanned(),
                    post.getCreatedAt()
                );
            })
            .toList();

        return new PostListResponse(
            content,
            Pagination.from(postPage)
        );
    }

    private String getFileExtension(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/tiff" -> "tiff";
            default -> "jpg";
        };
    }
}

