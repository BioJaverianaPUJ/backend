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
