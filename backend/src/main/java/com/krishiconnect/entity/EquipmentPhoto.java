package com.krishiconnect.entity;

import java.time.Instant;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Photos live in the database so the app stays a single deployable with no file storage to manage. */
@Entity
@Table(name = "equipment_photos")
@Getter
@Setter
@NoArgsConstructor
public class EquipmentPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @Column(name = "content_type", nullable = false, length = 40)
    private String contentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] data;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public EquipmentPhoto(Equipment equipment, String contentType, byte[] data, int sortOrder) {
        this.equipment = equipment;
        this.contentType = contentType;
        this.data = data;
        this.sortOrder = sortOrder;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
