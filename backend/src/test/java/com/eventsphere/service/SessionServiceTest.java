package com.eventsphere.service;

import com.eventsphere.dto.SessionDtos.SessionRequest;
import com.eventsphere.entity.Event;
import com.eventsphere.entity.Role;
import com.eventsphere.entity.Speaker;
import com.eventsphere.entity.User;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionServiceTest extends IntegrationTest {

    @Autowired SessionService sessionService;

    private static SessionRequest req(Speaker speaker, String room, LocalDateTime start, int minutes) {
        return new SessionRequest("Talk", null, speaker == null ? null : speaker.getId(), room, start, start.plusMinutes(minutes));
    }

    @Test
    @DisplayName("R9: a speaker cannot be booked into overlapping sessions, even across events")
    void speakerDoubleBookingRejected() {
        User organizer = user(Role.ORGANIZER);
        Speaker speaker = speaker("Asha Rao");
        Event a = publishedEvent(organizer, 10);
        Event b = publishedEvent(organizer, 10);
        LocalDateTime at = a.getStartDateTime().plusHours(1);
        sessionService.create(a.getId(), req(speaker, "Hall A", at, 60), organizer);

        assertThatThrownBy(() -> sessionService.create(b.getId(), req(speaker, "Hall B", at.plusMinutes(30), 60), organizer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Asha Rao is already speaking");

        // back-to-back is fine
        assertThat(sessionService.create(b.getId(), req(speaker, "Hall B", at.plusMinutes(60), 60), organizer)).isNotNull();
    }

    @Test
    @DisplayName("R10: two sessions of an event cannot share a room at the same time")
    void roomClashRejected() {
        User organizer = user(Role.ORGANIZER);
        Event event = publishedEvent(organizer, 10);
        LocalDateTime at = event.getStartDateTime();
        sessionService.create(event.getId(), req(null, "Hall A", at, 90), organizer);

        assertThatThrownBy(() -> sessionService.create(event.getId(), req(null, "hall a", at.plusMinutes(45), 30), organizer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    @DisplayName("R8: sessions must lie inside the event window")
    void sessionOutsideEventRejected() {
        User organizer = user(Role.ORGANIZER);
        Event event = publishedEvent(organizer, 10);
        assertThatThrownBy(() -> sessionService.create(event.getId(),
                req(null, null, event.getEndDateTime().minusMinutes(30), 60), organizer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("within the event time");
    }
}
