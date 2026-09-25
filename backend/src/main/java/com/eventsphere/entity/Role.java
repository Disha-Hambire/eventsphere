package com.eventsphere.entity;

/**
 * ADMIN       - platform owner: manages users/roles and can manage every event.
 * ORGANIZER   - creates and runs their own events (sessions, check-in, reports).
 * PARTICIPANT - registers for events, holds QR tickets, gives feedback.
 */
public enum Role {
    ADMIN,
    ORGANIZER,
    PARTICIPANT
}
