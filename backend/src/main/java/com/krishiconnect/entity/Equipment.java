package com.krishiconnect.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipment")
@Getter
@Setter
@NoArgsConstructor
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EquipmentCategory category;

    @Column(length = 60)
    private String brand;

    @Column(length = 60)
    private String model;

    @Column(name = "year_of_make")
    private Integer yearOfMake;

    @Column(name = "horse_power")
    private Integer horsePower;

    @Column(length = 1000)
    private String description;

    @Column(name = "price_per_hour", precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Column(name = "price_per_day", precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    @Column(name = "price_per_acre", precision = 10, scale = 2)
    private BigDecimal pricePerAcre;

    /** Price includes someone to run the machine. */
    @Column(name = "with_operator", nullable = false)
    private boolean withOperator;

    @Column(nullable = false, length = 80)
    private String village;

    @Column(length = 60)
    private String district;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EquipmentStatus status = EquipmentStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;

    @Column(name = "moderation_note")
    private String moderationNote;

    /** Kept on the row so search results don't need to aggregate reviews. */
    @Column(name = "rating_sum", nullable = false)
    private int ratingSum;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isBookable() {
        return status == EquipmentStatus.ACTIVE && moderationStatus == ModerationStatus.APPROVED;
    }

    public BigDecimal priceFor(PriceUnit unit) {
        return switch (unit) {
            case HOUR -> pricePerHour;
            case DAY -> pricePerDay;
            case ACRE -> pricePerAcre;
            case FIXED -> null;
        };
    }

    public Double averageRating() {
        return ratingCount == 0 ? null : Math.round(ratingSum * 10.0 / ratingCount) / 10.0;
    }
}
