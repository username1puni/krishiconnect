package com.krishiconnect.location;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;

/**
 * Turns "how far is it" into a road distance when possible.
 *
 * 1. The caller has already found candidates with a cheap bounding box + haversine query in MySQL,
 *    sorted nearest first.
 * 2. Only the first N of them are sent to the routing API, in one table request.
 * 3. Answers are cached per (rounded origin, destination) for a day; farmers in the same village
 *    share the cache.
 * 4. If the API fails or times out, everything falls back to the straight line distance, and the
 *    API is skipped for a cool-down period so every search doesn't wait for the same timeout.
 */
@Slf4j
@Service
public class DistanceService {

    private final RoutingClient routingClient;
    private final Clock clock;
    private final int maxDestinations;
    private final Duration cooldown;
    private final AtomicBoolean enabled;
    private final Cache<String, RouteLeg> cache;

    private final AtomicLong apiCalls = new AtomicLong();
    private final AtomicLong apiFailures = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private volatile Instant skipUntil = Instant.EPOCH;
    private volatile Instant lastSuccessAt;
    private volatile Instant lastFailureAt;
    private volatile String lastError;

    public DistanceService(RoutingClient routingClient, Clock clock,
                           @Value("${app.routing.enabled}") boolean enabled,
                           @Value("${app.routing.max-destinations}") int maxDestinations,
                           @Value("${app.routing.cooldown-seconds}") long cooldownSeconds,
                           @Value("${app.routing.cache-hours}") long cacheHours) {
        this.routingClient = routingClient;
        this.clock = clock;
        this.enabled = new AtomicBoolean(enabled);
        this.maxDestinations = maxDestinations;
        this.cooldown = Duration.ofSeconds(cooldownSeconds);
        this.cache = Caffeine.newBuilder()
                .maximumSize(20_000)
                .expireAfterWrite(Duration.ofHours(cacheHours))
                .build();
    }

    public record Target(long id, double lat, double lng, double straightKm) {
    }

    /**
     * @param targets candidates sorted by straight line distance, nearest first
     * @return distance for every target id
     */
    public Map<Long, Distance> measure(double originLat, double originLng, List<Target> targets) {
        Map<Long, Distance> result = new HashMap<>();
        for (Target t : targets) {
            result.put(t.id(), Distance.straight(t.straightKm()));
        }
        if (!enabled.get() || targets.isEmpty()) {
            return result;
        }

        List<Target> routed = targets.subList(0, Math.min(maxDestinations, targets.size()));
        List<Target> missing = new ArrayList<>();
        for (Target t : routed) {
            RouteLeg cached = cache.getIfPresent(key(originLat, originLng, t));
            if (cached != null) {
                cacheHits.incrementAndGet();
                result.put(t.id(), Distance.road(cached.meters(), cached.seconds()));
            } else {
                missing.add(t);
            }
        }
        if (missing.isEmpty() || clock.instant().isBefore(skipUntil)) {
            return result;
        }

        List<GeoPoint> points = missing.stream().map(t -> new GeoPoint(t.lat(), t.lng())).toList();
        try {
            apiCalls.incrementAndGet();
            List<RouteLeg> legs = routingClient.table(new GeoPoint(originLat, originLng), points);
            for (int i = 0; i < missing.size() && i < legs.size(); i++) {
                RouteLeg leg = legs.get(i);
                if (leg != null) {
                    Target t = missing.get(i);
                    cache.put(key(originLat, originLng, t), leg);
                    result.put(t.id(), Distance.road(leg.meters(), leg.seconds()));
                }
            }
            lastSuccessAt = clock.instant();
        } catch (RuntimeException e) {
            apiFailures.incrementAndGet();
            lastFailureAt = clock.instant();
            lastError = e.getMessage();
            skipUntil = clock.instant().plus(cooldown);
            log.warn("Routing API failed, using straight line distance for {}s: {}", cooldown.toSeconds(), e.getMessage());
        }
        return result;
    }

    public RoutingStatus status() {
        boolean coolingDown = clock.instant().isBefore(skipUntil);
        return new RoutingStatus(enabled.get(), coolingDown, maxDestinations, apiCalls.get(), apiFailures.get(),
                cacheHits.get(), cache.estimatedSize(), lastSuccessAt, lastFailureAt, lastError);
    }

    /** Admin switch, handy to show the fallback working in a demo. */
    public void setEnabled(boolean value) {
        enabled.set(value);
        if (value) {
            skipUntil = Instant.EPOCH;
        }
    }

    public void clearCache() {
        cache.invalidateAll();
    }

    // origin rounded to ~100 m so neighbours reuse each other's results
    private static String key(double lat, double lng, Target t) {
        return String.format(Locale.ROOT, "%.3f,%.3f>%.5f,%.5f", lat, lng, t.lat(), t.lng());
    }
}
