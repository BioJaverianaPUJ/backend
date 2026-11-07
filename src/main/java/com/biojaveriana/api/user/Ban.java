package com.biojaveriana.api.user;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder.Default;

@Entity
@Table(name = "bans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ban {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Column(name = "reason", nullable = false)
    private String reason;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ban_type", nullable = false)
    private BanType banType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Default
    private BanStatus status = BanStatus.ACTIVE;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @ManyToOne(optional = true)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
