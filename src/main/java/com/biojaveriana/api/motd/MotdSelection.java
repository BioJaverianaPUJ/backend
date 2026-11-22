package com.biojaveriana.api.motd;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "motd_selections", uniqueConstraints = {
    @UniqueConstraint(name = "uk_motd_selection_display_date", columnNames = "display_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotdSelection {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fun_fact_id", nullable = false, foreignKey = @ForeignKey(name = "fk_motdselection_funfact"))
    private FunFact funFact;

    @Column(name = "display_date", nullable = false, unique = true)
    private LocalDate displayDate;

    @CreationTimestamp
    @Column(name = "selected_at", nullable = false, updatable = false)
    private Instant selectedAt;
}
