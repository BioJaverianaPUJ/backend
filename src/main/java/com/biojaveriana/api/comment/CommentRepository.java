package com.biojaveriana.api.comment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Page<Comment> findByPostIdAndHiddenFalseAndBannedFalseOrderByCreatedAtDesc(UUID postId, Pageable pageable);

    Page<Comment> findByPostIdAndParentIsNullAndHiddenFalseAndBannedFalseOrderByCreatedAtDesc(UUID postId, Pageable pageable);

    Page<Comment> findByPostIdAndHiddenTrueOrBannedTrueOrderByCreatedAtDesc(UUID postId, Pageable pageable);

    Page<Comment> findByParentIdAndHiddenFalseAndBannedFalseOrderByCreatedAtDesc(UUID parentId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.hidden = false AND c.banned = false AND c.parent IS NULL ORDER BY c.createdAt DESC")
    Page<Comment> findRootCommentsByPostId(@Param("postId") UUID postId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.hidden = false AND c.banned = false ORDER BY c.createdAt DESC")
    Page<Comment> findAllActiveByPostId(@Param("postId") UUID postId, Pageable pageable);

    Optional<Comment> findByIdAndHiddenFalseAndBannedFalse(UUID id);

    List<Comment> findByParentId(UUID parentId);

    long countByPostId(UUID postId);

    long countByAuthorId(UUID authorId);

    long countByAuthorIdAndCreatedAtGreaterThanEqual(UUID authorId, Instant createdAt);

    @Query("SELECT c.id FROM Comment c WHERE c.author.id = :authorId")
    List<UUID> findIdsByAuthorId(@Param("authorId") UUID authorId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.parent.id IN :parentIds")
    void deleteByParentIdIn(@Param("parentIds") Collection<UUID> parentIds);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.author.id = :authorId")
    void deleteByAuthorId(@Param("authorId") UUID authorId);
}