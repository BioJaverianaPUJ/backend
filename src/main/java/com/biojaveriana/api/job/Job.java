package com.biojaveriana.api.job;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // --- Polymorphic ownership (Approach 1) ---
    // The type of entity that owns this job, e.g. "Observation", "SpeciesRecord"
    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    // The PK of the owning entity — nullable for jobs without a specific owner
    @Column(name = "resource_id", nullable = true)
    private UUID resourceId;

    @Column(name = "payload", nullable = true)
    private String payload;

    @Column(name = "result", nullable = true)
    private String result;

    @Column(name = "error_message", nullable = true)
    private String errorMessage;

    @Column(name = "retries", nullable = false)
    private int retries;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status;

    @Column(name = "queued_at", nullable = true)
    private Instant queuedAt;

    @Column(name = "started_at", nullable = true)
    private Instant startedAt;

    @Column(name = "completed_at", nullable = true)
    private Instant completedAt;

    @Column(name = "queue_key", nullable = false)
    private String queueKey;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // --- Convenience helpers ---

    /**
     * Returns the unique resource key used for queue routing:
     * e.g. "jobs:Observation:a3b1c2d3-e4f5..."
     */
    public String getQueueKeyForResource() {
        if (resourceId == null || resourceType == null) {
            return queueKey; // fall back to explicit queueKey set at creation time
        }
        return "jobs:" + resourceType + ":" + resourceId.toString();
    }

    /**
     * Checks if this job is the active/deferred job for its owning resource.
     */
    public boolean isDefered() {
        return status == JobStatus.COMPLETED || status == JobStatus.FAILED;
    }

    @PreUpdate
    public void onUpdate() {
        setUpdatedAt(Instant.now());
    }

    @PrePersist
    public void onCreate() {
        if (updatedAt == null) {
            setUpdatedAt(Instant.now());
        }
    }
}
