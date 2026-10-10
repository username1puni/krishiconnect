package com.krishiconnect.repository;

/** Row from a "near me" query: the id and the straight line distance worked out in SQL. */
public interface GeoHit {

    Long getId();

    Double getDistanceKm();
}
