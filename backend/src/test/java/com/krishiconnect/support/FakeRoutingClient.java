package com.krishiconnect.support;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.krishiconnect.location.GeoPoint;
import com.krishiconnect.location.RouteLeg;
import com.krishiconnect.location.RoutingClient;
import com.krishiconnect.location.RoutingException;

/** Stands in for OSRM in tests: answers with whatever the test sets, or fails on purpose. */
public class FakeRoutingClient implements RoutingClient {

    private volatile Function<GeoPoint, RouteLeg> answer = p -> null;
    private volatile boolean down;
    public final AtomicInteger calls = new AtomicInteger();
    public volatile int lastBatchSize;

    public void answerWith(Function<GeoPoint, RouteLeg> answer) {
        this.answer = answer;
        this.down = false;
    }

    public void goDown() {
        this.down = true;
    }

    public void reset() {
        answer = p -> null;
        down = false;
        calls.set(0);
        lastBatchSize = 0;
    }

    @Override
    public List<RouteLeg> table(GeoPoint origin, List<GeoPoint> destinations) {
        calls.incrementAndGet();
        lastBatchSize = destinations.size();
        if (down) {
            throw new RoutingException("connect timed out");
        }
        return destinations.stream().map(answer).toList();
    }
}
