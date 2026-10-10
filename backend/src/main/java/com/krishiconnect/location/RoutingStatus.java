package com.krishiconnect.location;

import java.time.Instant;

public record RoutingStatus(boolean enabled, boolean coolingDown, int maxDestinations, long apiCalls,
                            long apiFailures, long cacheHits, long cachedRoutes, Instant lastSuccessAt,
                            Instant lastFailureAt, String lastError) {
}
