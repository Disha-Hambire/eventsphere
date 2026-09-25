package com.eventsphere.service;

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

class RegistrationServiceTest extends IntegrationTest {

    @Autowired
    RegistrationService registrationService;

    @Test
    @DisplayName("R14/R15: confirmed while seats remain, then waitlisted in FIFO order")
    void confirmsUntilFullThenWaitlists() {
        Event event = publishedEvent(user(Role.ORGANIZER), 2);

        RegistrationResult a = registrationService.register(event.getId(), user(Role.PARTICIPANT));
        RegistrationResult b = registrationService.register(event.getId(), user(Role.PARTICIPANT));
        clock.set(NOW.plusMinutes(1));
        RegistrationResult c = registrationService.register(event.getId(), user(Role.PARTICIPANT));
        clock.set(NOW.plusMinutes(2));
        RegistrationResult d = registrationService.register(event.getId(), user(Role.PARTICIPANT));

        assertThat(a.registration().status()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(b.registration().status()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(a.registration().ticketCode()).startsWith("ES-");
        assertThat(c.registration().status()).isEqualTo(RegistrationStatus.WAITLISTED);
        assertThat(c.registration().waitlistPosition()).isEqualTo(1);
        assertThat(c.registration().ticketCode()).as("waitlisted people get no usable ticket").isNull();
        assertThat(d.registration().waitlistPosition()).isEqualTo(2);
        assertThat(d.message()).contains("#2 on the waitlist");
    }

    @Test
    @DisplayName("R16: cancelling a confirmed seat promotes the first person on the waitlist")
    void cancellationPromotesFirstWaitlisted() {
        Event event = publishedEvent(user(Role.ORGANIZER), 1);
        User holder = user(Role.PARTICIPANT);
        User first = user(Role.PARTICIPANT);
        User second = user(Role.PARTICIPANT);

        RegistrationResult seat = registrationService.register(event.getId(), holder);
        clock.set(NOW.plusMinutes(1));
        RegistrationResult w1 = registrationService.register(event.getId(), first);
        clock.set(NOW.plusMinutes(2));
        registrationService.register(event.getId(), second);

        RegistrationResult cancelled = registrationService.cancel(seat.registration().id(), holder);

        assertThat(cancelled.registration().status()).isEqualTo(RegistrationStatus.CANCELLED);
        assertThat(cancelled.promoted()).isNotNull();
        assertThat(cancelled.promoted().participantId()).isEqualTo(first.getId());
        assertThat(cancelled.message()).contains("automatically given to");

        Registration promoted = registrationRepository.findById(w1.registration().id()).orElseThrow();
        assertThat(promoted.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(promoted.isPromotedFromWaitlist()).isTrue();
        assertThat(registrationRepository.findByEventIdAndParticipantId(event.getId(), second.getId())
                .orElseThrow().getStatus()).isEqualTo(RegistrationStatus.WAITLISTED);
    }

    @Test
    @DisplayName("R13: a participant cannot register twice, but can re-register after cancelling")
    void duplicateRegistrationRejected() {
        Event event = publishedEvent(user(Role.ORGANIZER), 10);
        User p = user(Role.PARTICIPANT);
        RegistrationResult first = registrationService.register(event.getId(), p);

        assertThatThrownBy(() -> registrationService.register(event.getId(), p))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already have a confirmed seat");

        registrationService.cancel(first.registration().id(), p);
        RegistrationResult again = registrationService.register(event.getId(), p);
        assertThat(again.registration().id()).isEqualTo(first.registration().id());
        assertThat(again.registration().status()).isEqualTo(RegistrationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Re-joining a full event puts you at the back of the queue with the correct position")
    void rejoinGoesToBackOfWaitlist() {
        Event event = publishedEvent(user(Role.ORGANIZER), 1);
        User holder = user(Role.PARTICIPANT);
        User p = user(Role.PARTICIPANT);
        registrationService.register(event.getId(), holder);
        clock.set(NOW.plusMinutes(1));
        RegistrationResult mine = registrationService.register(event.getId(), p);
        clock.set(NOW.plusMinutes(2));
        registrationService.register(event.getId(), user(Role.PARTICIPANT));

        registrationService.cancel(mine.registration().id(), p);
        // sub-millisecond "now" exercises the DATETIME(6) precision edge case
        clock.set(NOW.plusMinutes(3).plusNanos(123_456_789));
        RegistrationResult again = registrationService.register(event.getId(), p);

        assertThat(again.registration().waitlistPosition()).isEqualTo(2);
        assertThat(again.message()).contains("#2 on the waitlist");
    }

    @Test
    @DisplayName("R12: registration closes at the deadline")
    void registrationClosedAfterDeadline() {
        Event event = publishedEvent(user(Role.ORGANIZER), 10);
        clock.set(event.getRegistrationDeadline().plusMinutes(1));

        assertThatThrownBy(() -> registrationService.register(event.getId(), user(Role.PARTICIPANT)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Registration closed");
    }

    @Test
    @DisplayName("R12: draft events do not accept registrations")
    void draftEventRejectsRegistration() {
        Event event = draftEvent(user(Role.ORGANIZER));
        assertThatThrownBy(() -> registrationService.register(event.getId(), user(Role.PARTICIPANT)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not open");
    }

    @Test
    @DisplayName("R16: a registration cannot be cancelled once the event has started")
    void cannotCancelAfterStart() {
        Event event = publishedEvent(user(Role.ORGANIZER), 10);
        User p = user(Role.PARTICIPANT);
        RegistrationResult r = registrationService.register(event.getId(), p);
        clock.set(event.getStartDateTime().plusMinutes(5));

        assertThatThrownBy(() -> registrationService.cancel(r.registration().id(), p))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already started");
    }

    @Test
    @DisplayName("Smart registration: cannot hold seats at two events that overlap in time")
    void overlappingEventsBlocked() {
        User organizer = user(Role.ORGANIZER);
        Event a = publishedEvent(organizer, 10);
        Event b = publishedEvent(organizer, 10); // same day, same hours
        User p = user(Role.PARTICIPANT);
        registrationService.register(a.getId(), p);

        assertThatThrownBy(() -> registrationService.register(b.getId(), p))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    @DisplayName("CSV export: every row has the same number of columns as the header")
    void csvColumnsLineUp() {
        User organizer = user(Role.ORGANIZER);
        Event event = publishedEvent(organizer, 1);
        registrationService.register(event.getId(), user(Role.PARTICIPANT));
        clock.set(NOW.plusMinutes(1));
        registrationService.register(event.getId(), user(Role.PARTICIPANT)); // waitlisted row

        String[] lines = registrationService.exportCsv(event.getId(), organizer).split("\n");
        int headerColumns = lines[0].split(",").length;
        for (int i = 1; i < lines.length; i++) {
            // split on commas that are outside double quotes
            int columns = lines[i].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1).length;
            assertThat(columns).as("row %d: %s", i, lines[i]).isEqualTo(headerColumns);
        }
    }

    @Test
    @DisplayName("Only participants can register")
    void organizersCannotRegister() {
        Event event = publishedEvent(user(Role.ORGANIZER), 10);
        assertThatThrownBy(() -> registrationService.register(event.getId(), user(Role.ORGANIZER)))
                .hasMessageContaining("Only participants");
    }
}
