package com.biojaveriana.api.comment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, UUID> {
    Optional<CommentReaction> findByCommentIdAndUserId(UUID commentId, UUID userId);

    @Query("SELECT cr FROM CommentReaction cr WHERE cr.comment.id = :commentId AND cr.user.id = :userId ORDER BY cr.createdAt DESC")
    List<CommentReaction> findByCommentIdAndUserIdOrderByCreatedAtDesc(@Param("commentId") UUID commentId, @Param("userId") UUID userId);

    List<CommentReaction> findByCommentId(UUID commentId);

    void deleteByCommentIdAndUserId(UUID commentId, UUID userId);

    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    @Query("SELECT COUNT(cr) FROM CommentReaction cr WHERE cr.comment.author.id = :userId")
    long countReactionsReceivedByUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(cr) FROM CommentReaction cr WHERE cr.comment.author.id = :userId AND cr.createdAt >= :since")
    long countReactionsReceivedByUserSince(@Param("userId") UUID userId, @Param("since") Instant since);

    @Modifying
    @Query("DELETE FROM CommentReaction cr WHERE cr.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM CommentReaction cr WHERE cr.comment.id IN :commentIds")
    void deleteByCommentIdIn(@Param("commentIds") Collection<UUID> commentIds);
}