package com.eventsphere.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * One row per (event, participant). A cancelled registration is re-activated rather than duplicated.
 * The ticket code is encoded in the participant's QR ticket and scanned at check-in.
 */
@Entity
@Table(name = "registrations",
        uniqueConstraints = @UniqueConstraint(name = "uk_registration_event_participant",
                columnNames = {"event_id", "participant_id"}))
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id")
    private User participant;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 40)
    private String ticketCode;

    /** Queue order: the waitlist is served first-come-first-served by this timestamp. */
    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /** True when the seat came from the waitlist (shown in the registration timeline). */
    @Column(name = "promoted_from_waitlist", nullable = false)
    private boolean promotedFromWaitlist;

    protected Registration() {
    }

    public Registration(Event event, User participant, RegistrationStatus status,
                        String ticketCode, LocalDateTime registeredAt) {
        this.event = event;
        this.participant = participant;
        this.status = status;
        this.ticketCode = ticketCode;
        this.registeredAt = micros(registeredAt);
        if (status == RegistrationStatus.CONFIRMED) {
            this.confirmedAt = this.registeredAt;
        }
    }

    public void confirm(LocalDateTime now) {
        this.status = RegistrationStatus.CONFIRMED;
        this.confirmedAt = micros(now);
        this.cancelledAt = null;
        this.promotedFromWaitlist = false;
    }

    public void promoteFromWaitlist(LocalDateTime now) {
        this.status = RegistrationStatus.CONFIRMED;
        this.confirmedAt = micros(now);
        this.promotedFromWaitlist = true;
    }

    /** Re-joining after a cancellation puts the participant at the back of the queue. */
    public void rejoinWaitlist(LocalDateTime now) {
        this.status = RegistrationStatus.WAITLISTED;
        this.registeredAt = micros(now);
        this.confirmedAt = null;
        this.cancelledAt = null;
        this.promotedFromWaitlist = false;
    }

    public void rejoinConfirmed(LocalDateTime now) {
        this.registeredAt = micros(now);
        confirm(now);
    }

    public void cancel(LocalDateTime now) {
        this.status = RegistrationStatus.CANCELLED;
        this.cancelledAt = micros(now);
    }

    /**
     * The database keeps microseconds (DATETIME(6)). Truncating here keeps the in-memory value identical to the
     * stored one, so FIFO comparisons (e.g. waitlist position) never see a row as "earlier than itself".
     */
    private static LocalDateTime micros(LocalDateTime t) {
        return t == null ? null : t.truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public User getParticipant() { return participant; }
    public RegistrationStatus getStatus() { return status; }
    public String getTicketCode() { return ticketCode; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public boolean isPromotedFromWaitlist() { return promotedFromWaitlist; }
}
