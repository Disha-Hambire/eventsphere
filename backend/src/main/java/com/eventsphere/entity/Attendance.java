package com.eventsphere.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * A check-in of a confirmed participant into one session (by QR scan or manually by the organizer).
 */
@Entity
@Table(name = "attendance",
        uniqueConstraints = @UniqueConstraint(name = "uk_attendance_registration_session",
                columnNames = {"registration_id", "session_id"}))
public class Attendance {

    public enum Method { QR, MANUAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id")
    private Registration registration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private EventSession session;

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Method method;

    protected Attendance() {
    }

    public Attendance(Registration registration, EventSession session, LocalDateTime checkedInAt, Method method) {
        this.registration = registration;
        this.session = session;
        this.checkedInAt = checkedInAt;
        this.method = method;
    }

    public Long getId() { return id; }
    public Registration getRegistration() { return registration; }
    public EventSession getSession() { return session; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public Method getMethod() { return method; }
}
