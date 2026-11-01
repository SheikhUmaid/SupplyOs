package com.supplychain.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Real time, unless the demonstration seeder has moved it into the past. History then passes through
 * the real services and gets genuine timestamps. Exists only in the dev profile.
 */
public class AdjustableClock extends Clock {

    private final Clock system;
    private volatile Instant fixed;

    public AdjustableClock(Clock system) {
        this.system = system;
    }

    public void travelTo(Instant instant) {
        fixed = instant;
    }

    public void resume() {
        fixed = null;
    }

    @Override
    public Instant instant() {
        Instant current = fixed;
        return current != null ? current : system.instant();
    }

    @Override
    public ZoneId getZone() {
        return system.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return system.withZone(zone);
    }
}
