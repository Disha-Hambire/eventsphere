package com.eventsphere.service;

import com.eventsphere.dto.AttendanceDtos.CheckInOutcome;
import com.eventsphere.dto.AttendanceDtos.CheckInRequest;
import com.eventsphere.dto.FeedbackDtos.FeedbackRequest;
import com.eventsphere.dto.RegistrationDtos.RegistrationResult;
import com.eventsphere.entity.*;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.exception.ConflictException;
import com.eventsphere.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttendanceAndFeedbackTest extends IntegrationTest {

    @Autowired RegistrationService registrationService;
    @Autowired AttendanceService attendanceService;
    @Autowired FeedbackService feedbackService;
    @Autowired EventService eventService;

    private static final FeedbackRequest GOOD = new FeedbackRequest(5, 4, 4, true, "Great keynote");

    @Test
    @DisplayName("QR check-in works once; scanning again reports ALREADY_CHECKED_IN instead of failing")
    void qrCheckInIsIdempotent() {
        User organizer = user(Role.ORGANIZER);
        Event event = publishedEvent(organizer, 10);
        EventSession session = session(event, null, "Hall", event.getStartDateTime(), 60);
        RegistrationResult r = registrationService.register(event.getId(), user(Role.PARTICIPANT));
        clock.set(session.getStartTime().minusMinutes(10));

        var first = attendanceService.checkInByTicket(new CheckInRequest(r.registration().ticketCode().toLowerCase(), session.getId()), organizer);
        var second = attendanceService.checkInByTicket(new CheckInRequest(r.registration().ticketCode(), session.getId()), organizer);

        assertThat(first.outcome()).isEqualTo(CheckInOutcome.CHECKED_IN);
        assertThat(second.outcome()).isEqualTo(CheckInOutcome.ALREADY_CHECKED_IN);
        assertThat(attendanceRepository.countBySessionId(session.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("R18: check-in opens 30 minutes before the session")
    void checkInWindowEnforced() {
        User organizer = user(Role.ORGANIZER);
        Event event = publishedEvent(organizer, 10);
        EventSession session = session(event, null, "Hall", event.getStartDateTime().plusHours(2), 60);
        RegistrationResult r = registrationService.register(event.getId(), user(Role.PARTICIPANT));
        clock.set(session.getStartTime().minusMinutes(45));

        assertThatThrownBy(() -> attendanceService.checkInByTicket(
                new CheckInRequest(r.registration().ticketCode(), session.getId()), organizer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("opens at");
    }

    @Test
    @DisplayName("R17: waitlisted participants cannot be checked in")
    void waitlistedCannotCheckIn() {
        User organizer = user(Role.ORGANIZER);
        Event event = publishedEvent(organizer, 1);
        EventSession session = session(event, null, "Hall", event.getStartDateTime(), 60);
        registrationService.register(event.getId(), user(Role.PARTICIPANT));
        User waiting = user(Role.PARTICIPANT);
        registrationService.register(event.getId(), waiting);
        Registration waitlisted = registrationRepository.findByEventIdAndParticipantId(event.getId(), waiting.getId()).orElseThrow();
        clock.set(session.getStartTime());

        assertThatThrownBy(() -> attendanceService.checkInByTicket(
                new CheckInRequest(waitlisted.getTicketCode(), session.getId()), organizer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("waitlist");
    }

    @Test
    @DisplayName("A ticket for one event cannot be used at another")
    void ticketForOtherEventRejected() {
        User organizer = user(Role.ORGANIZER);
        Event a = publishedEvent(organizer, 10);
        Event b = eventRepository.save(publishedEventCopy(a, organizer));
        EventSession sessionB = session(b, null, "Hall", b.getStartDateTime(), 60);
        RegistrationResult r = registrationService.register(a.getId(), user(Role.PARTICIPANT));
        clock.set(sessionB.getStartTime());

        assertThatThrownBy(() -> attendanceService.checkInByTicket(
                new CheckInRequest(r.registration().ticketCode(), sessionB.getId()), organizer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("This ticket is for");
    }

    @Test
    @DisplayName("R19/R20: feedback only after completion, only from attendees, only once")
    void feedbackRules() {
        User organizer = user(Role.ORGANIZER);
        Event event = publishedEvent(organizer, 10);
        EventSession session = session(event, null, "Hall", event.getStartDateTime(), 60);
        User attendee = user(Role.PARTICIPANT);
        User noShow = user(Role.PARTICIPANT);
        RegistrationResult r = registrationService.register(event.getId(), attendee);
        registrationService.register(event.getId(), noShow);

        clock.set(session.getStartTime());
        attendanceService.markPresent(session.getId(), r.registration().id(), organizer);

        assertThatThrownBy(() -> feedbackService.submit(event.getId(), GOOD, attendee))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("completed");

        eventService.complete(event.getId(), organizer);

        assertThatThrownBy(() -> feedbackService.submit(event.getId(), GOOD, noShow))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("attended");

        assertThat(feedbackService.submit(event.getId(), GOOD, attendee).rating()).isEqualTo(5);
        assertThatThrownBy(() -> feedbackService.submit(event.getId(), GOOD, attendee))
                .isInstanceOf(ConflictException.class);
    }

    private static Event publishedEventCopy(Event source, User organizer) {
        Event e = new Event(source.getTitle() + " (copy)", null, source.getCategory(), source.getVenue(),
                source.getStartDateTime(), source.getEndDateTime(), source.getRegistrationDeadline(), 10, organizer);
        e.setStatus(EventStatus.PUBLISHED);
        return e;
    }
}
