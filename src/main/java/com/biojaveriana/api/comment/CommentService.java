package com.biojaveriana.api.comment;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biojaveriana.api.comment.dto.CommentCreateRequest;
import com.biojaveriana.api.comment.dto.CommentListResponse;
import com.biojaveriana.api.comment.dto.CommentResponse;
import com.biojaveriana.api.comment.dto.CommentSummaryResponse;
import com.biojaveriana.api.comment.dto.CommentUpdateRequest;
import com.biojaveriana.api.comment.dto.ReactionSummaryResponse;
import com.biojaveriana.api.comment.dto.UserSummaryResponse;
import com.biojaveriana.api.comment.exceptions.CommentForbiddenException;
import com.biojaveriana.api.comment.exceptions.CommentNotFoundException;
import com.biojaveriana.api.common.Pagination;
import com.biojaveriana.api.post.Post;
import com.biojaveriana.api.post.PostRepository;
import com.biojaveriana.api.s3.S3UrlGenerator;
import com.biojaveriana.api.user.Role;
import com.biojaveriana.api.user.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final PostRepository postRepository;
    private final S3UrlGenerator s3UrlGenerator;

    @Transactional
    public CommentResponse create(UUID postId, User user, CommentCreateRequest request) {
        Post post = postRepository.findByIdAndHiddenFalseAndBannedFalse(postId)
            .orElseThrow(() -> new CommentNotFoundException(postId));

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                .orElseThrow(() -> new CommentNotFoundException(request.getParentId()));
            if (parent.getPost() != post) {
                throw new IllegalArgumentException("Parent comment does not belong to this post");
            }
            if (parent.getParent() != null) {
                throw new IllegalArgumentException("Cannot reply to a reply");
            }
            if (parent.getHidden() || parent.getBanned()) {
                throw new CommentNotFoundException(request.getParentId());
            }
        }

        var comment = Comment.builder()
            .post(post)
            .author(user)
            .parent(parent)
            .content(request.getContent())
            .totalReactions(0)
            .hidden(false)
            .banned(false)
            .build();

        comment = commentRepository.save(comment);
        log.info("Comment created id={} on post id={} by user id={}", comment.getId(), postId, user.getId());
        return toResponse(comment, null);
    }


    @Transactional
    public CommentResponse update(UUID commentId, User user, CommentUpdateRequest request) {
        Comment comment = findActiveComment(commentId);
        if (!isAuthorOrAdmin(comment.getAuthor(), user)) {
            throw new CommentForbiddenException();
        }
        comment.setContent(request.getContent());
        comment = commentRepository.save(comment);
        log.info("Comment updated id={} by user id={}", commentId, user.getId());
        return toResponse(comment, null);
    }

    @Transactional
    public void delete(UUID commentId, User user) {
        Comment comment = findActiveComment(commentId);
        if (!isAuthorOrAdmin(comment.getAuthor(), user)) {
            throw new CommentForbiddenException();
        }
        commentRepository.delete(comment);
        log.info("Comment deleted id={} by user id={}", commentId, user.getId());
    }


    @Transactional
    public CommentResponse toggleReaction(UUID commentId, User user, ReactionType emoji) {
        Comment comment = findActiveComment(commentId);
        java.util.Optional<CommentReaction> existingReaction = commentReactionRepository.findByCommentIdAndUserId(commentId, user.getId());
        if (existingReaction.isPresent()) {
            CommentReaction reaction = existingReaction.get();
            if (reaction.getEmoji() == emoji) {
                commentReactionRepository.delete(reaction);
                comment.setTotalReactions(Math.max(0, comment.getTotalReactions() - 1));
                comment = commentRepository.save(comment);
                log.info("Comment reaction removed commentId={} userId={} emoji={}", commentId, user.getId(), emoji);
                return toResponse(comment, null);
            } else {
                reaction.setEmoji(emoji);
                commentReactionRepository.save(reaction);
                log.info("Comment reaction updated commentId={} userId={}", commentId, user.getId());
                return toResponse(comment, null);
            }
        }
        var reaction = CommentReaction.builder()
            .comment(comment)
            .user(user)
            .emoji(emoji)
            .build();
        commentReactionRepository.save(reaction);
        comment.setTotalReactions(comment.getTotalReactions() + 1);
        comment = commentRepository.save(comment);
        log.info("Comment reaction created commentId={} userId={} emoji={}", commentId, user.getId(), emoji);
        return toResponse(comment, null);
    }


    @Transactional
    public void removeReaction(UUID commentId, User user) {
        CommentReaction reaction = commentReactionRepository.findByCommentIdAndUserId(commentId, user.getId())
            .orElseThrow(() -> new CommentNotFoundException(commentId));
        Comment comment = reaction.getComment();
        commentReactionRepository.delete(reaction);
        comment.setTotalReactions(Math.max(0, comment.getTotalReactions() - 1));
        commentRepository.save(comment);
        log.info("Comment reaction removed commentId={} userId={}", commentId, user.getId());
    }

    @Transactional
    public CommentResponse getById(UUID commentId, User user) {
        Comment comment = findActiveComment(commentId);
        return toResponse(comment, null);
    }

    @Transactional(readOnly = true)
    public CommentListResponse listByPost(UUID postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Comment> commentPage = commentRepository.findRootCommentsByPostId(postId, pageable);
        return toListResponse(commentPage);
    }

    @Transactional(readOnly = true)
    public CommentListResponse listReplies(UUID parentId, int page, int size) {
        findActiveComment(parentId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<Comment> commentPage = commentRepository.findByParentIdAndHiddenFalseAndBannedFalseOrderByCreatedAtDesc(parentId, pageable);
        return toListResponse(commentPage);
    }


    @Transactional
    public CommentResponse toggleHidden(UUID commentId, User user) {
        Comment comment = findActiveComment(commentId);
        if (!isAuthorOrAdmin(comment.getAuthor(), user)) {
            throw new CommentForbiddenException();
        }
        comment.setHidden(!comment.getHidden());
        comment = commentRepository.save(comment);
        log.info("Comment {} hidden status toggled to {} by user id={}", commentId, comment.getHidden(), user.getId());
        return toResponse(comment, null);
    }

    @Transactional
    public CommentResponse toggleBanned(UUID commentId, User user) {
        Comment comment = findActiveComment(commentId);
        if (!isAuthorOrAdmin(comment.getAuthor(), user)) {
            throw new CommentForbiddenException();
        }
        comment.setBanned(!comment.getBanned());
        comment = commentRepository.save(comment);
        log.info("Comment {} banned status toggled to {} by user id={}", commentId, comment.getBanned(), user.getId());
        return toResponse(comment, null);
    }

    @Transactional(readOnly = true)
    public long countByPostId(UUID postId) {
        return commentRepository.countByPostId(postId);
    }


    private Comment findActiveComment(UUID commentId) {
        return commentRepository.findByIdAndHiddenFalseAndBannedFalse(commentId)
            .orElseThrow(() -> new CommentNotFoundException(commentId));
    }

    private boolean isAuthorOrAdmin(User commentAuthor, User user) {
        return commentAuthor.getId().equals(user.getId()) || user.getRole() == Role.ADMIN;
    }


    private CommentResponse toResponse(Comment comment, CommentReaction existingReaction) {
        Map<ReactionType, List<UserSummaryResponse>> reactionMap = comment.getReactions() != null
            ? comment.getReactions().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    reaction -> reaction.getEmoji(),
                    java.util.stream.Collectors.mapping(
                        r -> new UserSummaryResponse(
                            r.getUser().getId(),
                            r.getUser().getUsername(),
                            r.getUser().getFirstName(),
                            r.getUser().getLastName(),
                            r.getUser().getProfilePictureKey() != null
                                ? s3UrlGenerator.generateUrl(r.getUser().getProfilePictureKey())
                                : null),
                        java.util.stream.Collectors.toList()
                    )
                ))
            : java.util.Map.of();

        List<ReactionSummaryResponse> reactions = reactionMap.entrySet().stream()
            .map(entry -> new ReactionSummaryResponse(
                entry.getKey(),
                entry.getValue().size(),
                entry.getValue()
            ))
            .sorted(Comparator.comparingInt(r -> r.emoji().ordinal()))
            .toList();


        return new CommentResponse(
            comment.getId(),
            new UserSummaryResponse(
                comment.getAuthor().getId(),
                comment.getAuthor().getUsername(),
                comment.getAuthor().getFirstName(),
                comment.getAuthor().getLastName(),
                comment.getAuthor().getProfilePictureKey() != null
                    ? s3UrlGenerator.generateUrl(comment.getAuthor().getProfilePictureKey())
                    : null),
            comment.getPost().getId(),
            comment.getParent() != null ? comment.getParent().getId() : null,
            comment.getContent(),
            comment.getTotalReactions(),
            reactions,
            comment.getBanned(),
            comment.getCreatedAt(),
            comment.getUpdatedAt()
        );
    }


    private CommentListResponse toListResponse(Page<Comment> commentPage) {
        List<CommentSummaryResponse> content = commentPage.getContent().stream()
            .map(comment -> new CommentSummaryResponse(
                comment.getId(),
                new UserSummaryResponse(
                    comment.getAuthor().getId(),
                    comment.getAuthor().getUsername(),
                    comment.getAuthor().getFirstName(),
                    comment.getAuthor().getLastName(),
                    comment.getAuthor().getProfilePictureKey() != null
                        ? s3UrlGenerator.generateUrl(comment.getAuthor().getProfilePictureKey())
                        : null),
                comment.getPost().getId(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getContent(),
                comment.getTotalReactions(),
                comment.getBanned(),
                comment.getCreatedAt()
            ))
            .toList();

        return new CommentListResponse(content, Pagination.from(commentPage));
    }
}

