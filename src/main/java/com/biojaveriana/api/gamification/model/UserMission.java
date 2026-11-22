package com.biojaveriana.api.gamification.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A per-user, per-period instance of a {@link Mission}. The
 * ({@code periodStart}, {@code periodEnd}) pair is the evaluation window: the
 * mission's predicate is only satisfied by activity inside it. For assigned
 * DAILY/WEEKLY missions the window is the calendar day/week; for EVENT
 * missions it is the mission's fixed window.
 */
@Entity
@Table(name = "user_missions", indexes = {
    @Index(name = "idx_user_missions_user_id", columnList = "user_id"),
    @Index(name = "idx_user_missions_mission_id", columnList = "mission_id"),
    @Index(name = "idx_user_missions_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_missions_user_mission_period", columnNames = {"user_id", "mission_id", "period_start"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MissionStatus status = MissionStatus.ACTIVE;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "xp_awarded", nullable = false)
    @Builder.Default
    private Long xpAwarded = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
