package com.krishiconnect.location;

public final class GeoUtils {

    public static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    /** Great circle ("as the crow flies") distance between two points, in km. */
    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    /**
     * Smallest lat/lng rectangle that contains a circle of radiusKm around the point. One degree of
     * latitude is about 111 km everywhere; a degree of longitude shrinks with cos(latitude).
     */
    public static BoundingBox boundingBox(double lat, double lng, double radiusKm) {
        double dLat = Math.toDegrees(radiusKm / EARTH_RADIUS_KM);
        double dLng = Math.toDegrees(radiusKm / (EARTH_RADIUS_KM * Math.cos(Math.toRadians(lat))));
        return new BoundingBox(lat - dLat, lat + dLat, lng - dLng, lng + dLng);
    }

    public static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    public record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {
    }
}
