package com.eventsphere.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/**
 * All business rules read "now" from this Clock, so tests can freeze or move time.
 * It ticks in microseconds - the precision of DATETIME(6) - so a timestamp held in memory is exactly
 * equal to the stored one. (Otherwise, e.g., a participant could count themselves "ahead" of themselves
 * in the waitlist, because the database had truncated the extra nanoseconds.)
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.tick(Clock.systemDefaultZone(), Duration.ofNanos(1_000));
    }
}
