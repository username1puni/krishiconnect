package com.krishiconnect.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "places")
@Getter
@NoArgsConstructor
public class Place {

    @Id
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 60)
    private String district;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;
}
