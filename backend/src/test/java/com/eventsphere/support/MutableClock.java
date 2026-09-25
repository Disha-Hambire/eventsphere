package com.eventsphere.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** A clock tests can move, so time-based rules (deadlines, check-in windows) can be verified. */
public class MutableClock extends Clock {

    private Instant instant;
    private final ZoneId zone;

    public MutableClock(LocalDateTime start, ZoneId zone) {
        this.zone = zone;
        set(start);
    }

    public void set(LocalDateTime time) {
        this.instant = time.atZone(zone).toInstant();
    }

    public LocalDateTime now() {
        return LocalDateTime.ofInstant(instant, zone);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(now(), zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
