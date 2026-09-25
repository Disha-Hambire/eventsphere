package com.eventsphere.service;

import com.eventsphere.dto.EventDtos.EventRequest;
import com.eventsphere.entity.*;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.exception.ForbiddenException;
import com.eventsphere.exception.NotFoundException;
import com.eventsphere.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventServiceTest extends IntegrationTest {

    @Autowired EventService eventService;
    @Autowired RegistrationService registrationService;

    private static EventRequest requestFrom(Event e, int capacity) {
        return new EventRequest(e.getTitle(), e.getDescription(), e.getCategory(), e.getVenue(),
                e.getStartDateTime(), e.getEndDateTime(), e.getRegistrationDeadline(), capacity);
    }

    @Test
    @DisplayName("R2: an event without sessions cannot be published")
    void publishRequiresSession() {
        User organizer = user(Role.ORGANIZER);
        Event draft = draftEvent(organizer);

        assertThatThrownBy(() -> eventService.publish(draft.getId(), organizer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least one session");

        session(draft, null, null, draft.getStartDateTime(), 60);
        assertThat(eventService.publish(draft.getId(), organizer).status()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    @DisplayName("R1: the registration deadline must be before the event starts")
    void deadlineAfterStartRejected() {
        User organizer = user(Role.ORGANIZER);
        var start = NOW.plusDays(5);
        var req = new EventRequest("Bad", null, EventCategory.MEETUP, "Hall", start, start.plusHours(3),
                start.plusHours(1), 10);
        assertThatThrownBy(() -> eventService.create(req, organizer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("deadline");
    }

    @Test
    @DisplayName("R3: capacity cannot be reduced below confirmed participants")
    void capacityCannotDropBelowConfirmed() {
        User organizer = user(Role.ORGANIZER);
        Event event = publishedEvent(organizer, 3);
        for (int i = 0; i < 3; i++) {
            registrationService.register(event.getId(), user(Role.PARTICIPANT));
        }
        assertThatThrownBy(() -> eventService.update(event.getId(), requestFrom(event, 2), organizer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("3 participants are already confirmed");
    }

    @Test
    @DisplayName("R4: increasing capacity promotes waitlisted participants automatically")
    void capacityIncreasePromotesWaitlist() {
        User organizer = user(Role.ORGANIZER);
        Event event = publishedEvent(organizer, 1);
        registrationService.register(event.getId(), user(Role.PARTICIPANT));
        clock.set(NOW.plusMinutes(1));
        registrationService.register(event.getId(), user(Role.PARTICIPANT));
        clock.set(NOW.plusMinutes(2));
        registrationService.register(event.getId(), user(Role.PARTICIPANT));

        var updated = eventService.update(event.getId(), requestFrom(event, 2), organizer);

        assertThat(updated.confirmedCount()).isEqualTo(2);
        assertThat(updated.waitlistCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("R6: only draft events can be deleted")
    void onlyDraftsCanBeDeleted() {
        User organizer = user(Role.ORGANIZER);
        Event published = publishedEvent(organizer, 10);
        assertThatThrownBy(() -> eventService.delete(published.getId(), organizer))
                .isInstanceOf(BusinessRuleException.class);

        Event draft = draftEvent(organizer);
        eventService.delete(draft.getId(), organizer);
        assertThat(eventRepository.findById(draft.getId())).isEmpty();
    }

    @Test
    @DisplayName("R7: an event cannot be completed before it starts")
    void cannotCompleteBeforeStart() {
        User organizer = user(Role.ORGANIZER);
        Event event = publishedEvent(organizer, 10);
        assertThatThrownBy(() -> eventService.complete(event.getId(), organizer))
                .isInstanceOf(BusinessRuleException.class);

        clock.set(event.getStartDateTime().plusHours(1));
        assertThat(eventService.complete(event.getId(), organizer).status()).isEqualTo(EventStatus.COMPLETED);
    }

    @Test
    @DisplayName("Ownership: an organizer cannot manage another organizer's event; an admin can")
    void ownershipEnforced() {
        Event event = draftEvent(user(Role.ORGANIZER));
        assertThatThrownBy(() -> eventService.cancel(event.getId(), user(Role.ORGANIZER)))
                .isInstanceOf(ForbiddenException.class);
        assertThat(eventService.cancel(event.getId(), user(Role.ADMIN)).status()).isEqualTo(EventStatus.CANCELLED);
    }

    @Test
    @DisplayName("Drafts are invisible to participants")
    void draftsHiddenFromParticipants() {
        Event draft = draftEvent(user(Role.ORGANIZER));
        assertThatThrownBy(() -> eventService.detail(draft.getId(), user(Role.PARTICIPANT)))
                .isInstanceOf(NotFoundException.class);
        assertThat(eventService.list(user(Role.PARTICIPANT), null, null, null))
                .noneMatch(e -> e.id().equals(draft.getId()));
    }
}
