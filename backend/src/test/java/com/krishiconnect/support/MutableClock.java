package com.krishiconnect.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** A clock the tests can move forward, used to make bookings expire without waiting. */
public class MutableClock extends Clock {

    private final ZoneId zone;
    private Instant now;

    public MutableClock(Instant now, ZoneId zone) {
        this.now = now;
        this.zone = zone;
    }

    public synchronized void advance(Duration duration) {
        now = now.plus(duration);
    }

    public synchronized void reset() {
        now = Instant.now();
    }

    @Override
    public synchronized Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(now, zone);
    }
}
