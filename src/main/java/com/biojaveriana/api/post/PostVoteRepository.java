package com.biojaveriana.api.post;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostVoteRepository extends JpaRepository<PostVote, UUID> {
    Optional<PostVote> findByPostIdAndVoterId(UUID postId, UUID voterId);

    void deleteByPostIdAndVoterId(UUID postId, UUID voterId);

    @Modifying
    @Query("DELETE FROM PostVote pv WHERE pv.voter.id = :voterId")
    void deleteByVoterId(@Param("voterId") UUID voterId);

    @Modifying
    @Query("DELETE FROM PostVote pv WHERE pv.post.id IN :postIds")
    void deleteByPostIdIn(@Param("postIds") Collection<UUID> postIds);
}
