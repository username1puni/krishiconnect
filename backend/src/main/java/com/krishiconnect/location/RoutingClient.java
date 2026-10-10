package com.krishiconnect.location;

import java.util.List;

public interface RoutingClient {

    /**
     * Road distances from one origin to many destinations in a single call. The returned list has
     * one entry per destination, in the same order; an entry is null when no route was found.
     *
     * @throws RoutingException when the service is down, slow or answers with an error
     */
    List<RouteLeg> table(GeoPoint origin, List<GeoPoint> destinations);
}
