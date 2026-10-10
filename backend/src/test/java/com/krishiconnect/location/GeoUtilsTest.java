package com.krishiconnect.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {

    @Test
    void mandyaToMysuruIsAbout38KmInAStraightLine() {
        double km = GeoUtils.haversineKm(12.5223, 76.8970, 12.2958, 76.6394);
        assertThat(km).isCloseTo(37.6, within(1.0));
    }

    @Test
    void samePointIsZero() {
        assertThat(GeoUtils.haversineKm(12.5, 76.9, 12.5, 76.9)).isZero();
    }

    @Test
    void boundingBoxContainsTheWholeCircle() {
        var box = GeoUtils.boundingBox(12.5223, 76.8970, 20);
        // points 20 km due north and due east are on the edge of the box
        assertThat(GeoUtils.haversineKm(12.5223, 76.8970, box.maxLat(), 76.8970)).isCloseTo(20, within(0.05));
        assertThat(GeoUtils.haversineKm(12.5223, 76.8970, 12.5223, box.maxLng())).isCloseTo(20, within(0.1));
        // a degree of longitude is shorter than a degree of latitude away from the equator
        assertThat(box.maxLng() - 76.8970).isGreaterThan(box.maxLat() - 12.5223);
    }
}
