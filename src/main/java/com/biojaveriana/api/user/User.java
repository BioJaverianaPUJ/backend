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
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(min = 3, max = 25)
    @Column(nullable = false, unique = true)
    private String username;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotBlank
    @Size(min = 1, max = 50)
    @Column(nullable = false)
    private String firstName;

    @NotBlank
    @Size(min = 1, max = 50)
    @Column(nullable = false)
    private String lastName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "username_last_update")
    private Instant usernameLastUpdate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "profile_picture_key")
    private String profilePictureKey;

    @Column(name = "banner_key")
    private String bannerKey;

    @Column(name = "last_observation_time")
    private Instant lastObservationTime;

    /**
     * True if this is the auto-generated superadmin account.
     * The superadmin bypasses all role checks and cannot be banned,
     * modified, deleted, or replicated: only this account holds Role.SUPERADMIN.
     */
    public boolean isSuperAdmin() {
        return role == Role.SUPERADMIN;
    }

    /**
     * True if this user holds the ADMIN or SUPERADMIN role.
     * Centralizes the "admin or higher" permission check in the service layer so
     * the superadmin keeps bypassing every admin-gated action (mirrors the
     * SUPERADMIN bypass in RoleGuardInterceptor).
     */
    public boolean isAdminOrAbove() {
        return role == Role.ADMIN || role == Role.SUPERADMIN;
    }
}
