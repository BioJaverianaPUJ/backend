package com.biojaveriana.api.gamification.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An admin-defined mission template. Like achievements, missions are driven
 * by a metric predicate, but they are temporal: DAILY and WEEKLY missions are
 * assigned to users for the current calendar period, while EVENT missions are
 * shared by all users during a fixed window ({@code startTime} to
 * {@code endTime}).
 */
@Entity
@Table(name = "missions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "xp_reward", nullable = false)
    private Integer xpReward;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "predicate_json", columnDefinition = "jsonb")
    private String predicateJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private MissionType type;

    /**
     * Start of the fixed window for EVENT missions. Ignored for DAILY and
     * WEEKLY missions, whose window is derived from the user's assignment.
     */
    @Column(name = "start_time")
    private Instant startTime;

    /**
     * End of the fixed window for EVENT missions. Ignored for DAILY and
     * WEEKLY missions.
     */
    @Column(name = "end_time")
    private Instant endTime;

    @Column(nullable = false)
    @Default
    @ColumnDefault("true")
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
