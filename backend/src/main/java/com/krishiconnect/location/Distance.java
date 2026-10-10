package com.krishiconnect.location;

public record Distance(double km, Integer minutes, DistanceSource source) {

    public static Distance straight(double km) {
        return new Distance(GeoUtils.round(km, 1), null, DistanceSource.STRAIGHT_LINE);
    }

    public static Distance road(double meters, double seconds) {
        return new Distance(GeoUtils.round(meters / 1000.0, 1), (int) Math.round(seconds / 60.0), DistanceSource.ROAD);
    }
}
