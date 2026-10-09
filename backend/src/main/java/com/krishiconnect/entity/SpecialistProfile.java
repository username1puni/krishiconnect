package com.krishiconnect.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Extra details for users with the SPECIALIST role. The row is also what gets locked when a
 * specialist is booked, the same way an equipment row is locked for equipment.
 */
@Entity
@Table(name = "specialist_profiles")
@Getter
@Setter
@NoArgsConstructor
public class SpecialistProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Skill skill;

    @Column(length = 500)
    private String bio;

    @Column(name = "experience_years", nullable = false)
    private int experienceYears;

    @Column(name = "rate_per_hour", precision = 10, scale = 2)
    private BigDecimal ratePerHour;

    @Column(name = "rate_per_day", precision = 10, scale = 2)
    private BigDecimal ratePerDay;

    @Column(length = 100)
    private String languages;

    @Column(nullable = false)
    private boolean available = true;

    /** Kept on the row so search results don't need to aggregate reviews. */
    @Column(name = "rating_sum", nullable = false)
    private int ratingSum;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SpecialistProfile(User user, Skill skill) {
        this.user = user;
        this.skill = skill;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public Double averageRating() {
        return ratingCount == 0 ? null : Math.round(ratingSum * 10.0 / ratingCount) / 10.0;
    }
}
