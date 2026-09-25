package com.eventsphere.entity;

/**
 * Event lifecycle: DRAFT -> PUBLISHED -> COMPLETED, and DRAFT/PUBLISHED -> CANCELLED.
 */
public enum EventStatus {
    DRAFT,
    PUBLISHED,
    COMPLETED,
    CANCELLED
}
