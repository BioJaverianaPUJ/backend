package com.biojaveriana.api.post;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.biojaveriana.api.user.User;

public interface PostRepository extends JpaRepository<Post, UUID> {
    Page<Post> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);

    Page<Post> findByHiddenFalseAndBannedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByHiddenTrueOrBannedTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByAuthorIdAndHiddenFalseAndBannedFalseOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    @Query("SELECT COUNT(pv) FROM PostVote pv WHERE pv.post.id = :postId AND pv.voteType = :voteType")
    Integer countByPostIdAndVoteType(@Param("postId") UUID postId, @Param("voteType") VoteType voteType);

    Optional<Post> findByIdAndHiddenFalseAndBannedFalse(UUID id);

    long countByAuthorId(UUID authorId);

    @Query("SELECT p.id FROM Post p WHERE p.author.id = :authorId")
    List<UUID> findIdsByAuthorId(@Param("authorId") UUID authorId);

    @Modifying
    @Query("DELETE FROM Post p WHERE p.author.id = :authorId")
    void deleteByAuthorId(@Param("authorId") UUID authorId);
}