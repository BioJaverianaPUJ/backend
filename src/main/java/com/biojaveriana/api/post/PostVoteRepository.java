package com.biojaveriana.api.post;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostVoteRepository extends JpaRepository<PostVote, UUID> {
    Optional<PostVote> findByPostIdAndVoterId(UUID postId, UUID voterId);

    void deleteByPostIdAndVoterId(UUID postId, UUID voterId);
}
