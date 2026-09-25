package com.eventsphere.support;

import com.eventsphere.entity.*;
import com.eventsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for service-level integration tests: real Spring context, real Flyway schema on H2,
 * a controllable clock, and each test rolled back afterwards.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(IntegrationTest.ClockConfig.class)
public abstract class IntegrationTest {

    /** "Now" in every test: 1 Oct 2026, 10:00. */
    protected static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 10, 0);

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(NOW, ZoneId.systemDefault());
        }
    }

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired protected MutableClock clock;
    @Autowired protected UserRepository userRepository;
    @Autowired protected SpeakerRepository speakerRepository;
    @Autowired protected EventRepository eventRepository;
    @Autowired protected EventSessionRepository sessionRepository;
    @Autowired protected RegistrationRepository registrationRepository;
    @Autowired protected AttendanceRepository attendanceRepository;

    @BeforeEach
    void resetClock() {
        clock.set(NOW);
    }

    protected User user(Role role) {
        int n = SEQ.incrementAndGet();
        return userRepository.save(new User(role.name().toLowerCase() + n, role.name().toLowerCase() + n + "@test.com",
                "{noop}x", role));
    }

    protected Speaker speaker(String name) {
        return speakerRepository.save(new Speaker(name, name.replace(" ", ".").toLowerCase() + SEQ.incrementAndGet() + "@test.com",
                null, null, null, null));
    }

    /** A published event 10 days from now (09:00-18:00), registration open until the day before. */
    protected Event publishedEvent(User organizer, int capacity) {
        LocalDateTime start = NOW.plusDays(10).withHour(9);
        Event e = new Event("Event " + SEQ.incrementAndGet(), null, EventCategory.CONFERENCE, "Hall",
                start, start.withHour(18), start.minusDays(1), capacity, organizer);
        e.setStatus(EventStatus.PUBLISHED);
        return eventRepository.save(e);
    }

    protected Event draftEvent(User organizer) {
        LocalDateTime start = NOW.plusDays(10).withHour(9);
        return eventRepository.save(new Event("Draft " + SEQ.incrementAndGet(), null, EventCategory.MEETUP, "Hall",
                start, start.withHour(18), start.minusDays(1), 50, organizer));
    }

    protected EventSession session(Event event, Speaker speaker, String room, LocalDateTime start, int minutes) {
        EventSession s = new EventSession(event, "Session " + SEQ.incrementAndGet(), null, speaker, room,
                start, start.plusMinutes(minutes));
        event.getSessions().add(s);
        return sessionRepository.save(s);
    }
}
