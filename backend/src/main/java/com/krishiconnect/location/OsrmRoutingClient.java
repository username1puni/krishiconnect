package com.krishiconnect.location;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Uses the OSRM "table" service: one request returns the driving distance from the farmer to
 * every candidate, instead of one request per listing.
 * Docs: https://project-osrm.org/docs/v5.24.0/api/#table-service
 */
@Component
public class OsrmRoutingClient implements RoutingClient {

    private final RestClient restClient;

    public OsrmRoutingClient(@Value("${app.routing.base-url}") String baseUrl,
                             @Value("${app.routing.timeout-ms}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.min(timeoutMs, 1500));
        factory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("User-Agent", "KrishiConnect/1.0 (student project)")
                .build();
    }

    @Override
    public List<RouteLeg> table(GeoPoint origin, List<GeoPoint> destinations) {
        if (destinations.isEmpty()) {
            return List.of();
        }
        StringBuilder coords = new StringBuilder(format(origin));
        for (GeoPoint d : destinations) {
            coords.append(';').append(format(d));
        }
        JsonNode body;
        try {
            body = restClient.get()
                    .uri("/table/v1/driving/" + coords + "?sources=0&annotations=distance,duration")
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new RoutingException("OSRM request failed: " + e.getMessage(), e);
        }
        if (body == null || !"Ok".equals(body.path("code").asText())) {
            throw new RoutingException("OSRM answered " + (body == null ? "nothing" : body.path("code").asText()));
        }
        JsonNode distances = body.path("distances").path(0);
        JsonNode durations = body.path("durations").path(0);
        List<RouteLeg> legs = new ArrayList<>(destinations.size());
        // index 0 is the origin itself
        for (int i = 1; i <= destinations.size(); i++) {
            JsonNode m = distances.path(i);
            JsonNode s = durations.path(i);
            legs.add(m.isNumber() && s.isNumber() ? new RouteLeg(m.asDouble(), s.asDouble()) : null);
        }
        return legs;
    }

    // OSRM wants longitude first
    private static String format(GeoPoint p) {
        return String.format(Locale.ROOT, "%.6f,%.6f", p.lng(), p.lat());
    }
}
