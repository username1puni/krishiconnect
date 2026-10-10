package com.krishiconnect.location;

public enum DistanceSource {
    /** Driving distance from the routing API (or its cache). */
    ROAD,
    /** Haversine distance, used when the routing API is off, slow or failing. */
    STRAIGHT_LINE
}
