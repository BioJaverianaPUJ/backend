package com.biojaveriana.api.comment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, UUID> {
    Optional<CommentReaction> findByCommentIdAndUserId(UUID commentId, UUID userId);

    @Query("SELECT cr FROM CommentReaction cr WHERE cr.comment.id = :commentId AND cr.user.id = :userId ORDER BY cr.createdAt DESC")
    List<CommentReaction> findByCommentIdAndUserIdOrderByCreatedAtDesc(@Param("commentId") UUID commentId, @Param("userId") UUID userId);

    List<CommentReaction> findByCommentId(UUID commentId);

    void deleteByCommentIdAndUserId(UUID commentId, UUID userId);

    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);
}
