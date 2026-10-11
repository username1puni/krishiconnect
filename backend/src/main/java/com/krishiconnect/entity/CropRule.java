package com.krishiconnect.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "crop_rules")
@Getter
@Setter
@NoArgsConstructor
public class CropRule {

    @Id
    private Long id;

    @Column(nullable = false, length = 30)
    private String crop;

    @Column(nullable = false, length = 20)
    private String season;

    @Column(nullable = false, length = 20)
    private String activity;

    @Column(name = "month_from", nullable = false)
    private int monthFrom;

    @Column(name = "month_to", nullable = false)
    private int monthTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_category", length = 30)
    private EquipmentCategory equipmentCategory;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Skill skill;

    @Column(nullable = false)
    private String tip;

    /** True if the month falls in this rule's window, including windows like Nov to Jan. */
    public boolean covers(int month) {
        return monthFrom <= monthTo
                ? month >= monthFrom && month <= monthTo
                : month >= monthFrom || month <= monthTo;
    }
}
