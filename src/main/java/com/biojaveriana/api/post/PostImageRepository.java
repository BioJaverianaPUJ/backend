package com.biojaveriana.api.post;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostImageRepository extends JpaRepository<PostImage, UUID> {
    List<PostImage> findByPostOrderByOrderIndexAsc(Post post);

    void deleteByPostId(UUID postId);

    @Query("SELECT pi.s3Key FROM PostImage pi WHERE pi.post.id IN :postIds")
    List<String> findS3KeysByPostIdIn(@Param("postIds") Collection<UUID> postIds);

    @Modifying
    @Query("DELETE FROM PostImage pi WHERE pi.post.id IN :postIds")
    void deleteByPostIdIn(@Param("postIds") Collection<UUID> postIds);
}
