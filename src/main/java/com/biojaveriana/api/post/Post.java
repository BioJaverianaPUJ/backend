package com.biojaveriana.api.post;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.biojaveriana.api.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_post_author"))
    private User author;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "observation")
    private String observation;

    @Default
    @Column(name = "total_likes", nullable = false)
    private Integer totalLikes = 0;

    @Default
    @Column(name = "total_dislikes", nullable = false)
    private Integer totalDislikes = 0;

    @Default
    @Column(name = "hidden", nullable = false)
    private Boolean hidden = false;

    @Default
    @Column(name = "banned", nullable = false)
    private Boolean banned = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "post")
    @Builder.Default
    private java.util.List<PostImage> images = new java.util.ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "post")
    @Builder.Default
    private java.util.List<PostVote> votes = new java.util.ArrayList<>();
}
