package com.krishiconnect.location;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DistanceServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T06:00:00Z"), ZoneOffset.UTC);

    @Test
    void onlyTheNearestNAreSentToTheApi() {
        List<Integer> batchSizes = new ArrayList<>();
        RoutingClient client = (origin, destinations) -> {
            batchSizes.add(destinations.size());
            return destinations.stream().map(d -> new RouteLeg(10_000, 900)).toList();
        };
        DistanceService service = new DistanceService(client, clock, true, 3, 60, 24);
        List<DistanceService.Target> targets = List.of(target(1, 1.0), target(2, 2.0), target(3, 3.0), target(4, 4.0),
                target(5, 5.0));

        Map<Long, Distance> result = service.measure(12.5, 76.9, targets);

        assertThat(batchSizes).containsExactly(3);
        assertThat(result.get(1L).source()).isEqualTo(DistanceSource.ROAD);
        assertThat(result.get(3L).km()).isEqualTo(10.0);
        assertThat(result.get(4L).source()).isEqualTo(DistanceSource.STRAIGHT_LINE);
        assertThat(result.get(5L).km()).isEqualTo(5.0);
    }

    @Test
    void aMissingRouteFallsBackForThatOneDestination() {
        RoutingClient client = (origin, destinations) -> {
            List<RouteLeg> legs = new ArrayList<>();
            legs.add(new RouteLeg(8_000, 600));
            legs.add(null); // island, no road
            return legs;
        };
        DistanceService service = new DistanceService(client, clock, true, 25, 60, 24);

        Map<Long, Distance> result = service.measure(12.5, 76.9, List.of(target(1, 5.0), target(2, 6.0)));

        assertThat(result.get(1L).source()).isEqualTo(DistanceSource.ROAD);
        assertThat(result.get(2L).source()).isEqualTo(DistanceSource.STRAIGHT_LINE);
    }

    @Test
    void disabledRoutingNeverCallsTheApi() {
        RoutingClient client = (origin, destinations) -> {
            throw new AssertionError("should not be called");
        };
        DistanceService service = new DistanceService(client, clock, false, 25, 60, 24);

        Map<Long, Distance> result = service.measure(12.5, 76.9, List.of(target(1, 5.04)));

        assertThat(result.get(1L)).isEqualTo(Distance.straight(5.04));
        assertThat(result.get(1L).km()).isEqualTo(5.0);
    }

    @Test
    void errorsAreRecordedInTheStatus() {
        RoutingClient client = (origin, destinations) -> {
            throw new RoutingException("OSRM answered TooBig");
        };
        DistanceService service = new DistanceService(client, clock, true, 25, 60, 24);

        service.measure(12.5, 76.9, List.of(target(1, 5.0)));

        RoutingStatus status = service.status();
        assertThat(status.apiFailures()).isEqualTo(1);
        assertThat(status.coolingDown()).isTrue();
        assertThat(status.lastError()).contains("TooBig");
    }

    private static DistanceService.Target target(long id, double km) {
        return new DistanceService.Target(id, 12.5 + id / 100.0, 76.9, km);
    }
}
