package com.eventsphere.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * A participant's application to become an organizer. PENDING -> APPROVED (role becomes ORGANIZER) or REJECTED.
 */
@Entity
@Table(name = "organizer_requests")
public class OrganizerRequest {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 160)
    private String organization;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private User decidedBy;

    protected OrganizerRequest() {
    }

    public OrganizerRequest(User user, String organization, String reason, LocalDateTime createdAt) {
        this.user = user;
        this.organization = organization;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public void decide(Status decision, User admin, String note, LocalDateTime now) {
        this.status = decision;
        this.decidedBy = admin;
        this.adminNote = note;
        this.decidedAt = now;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getOrganization() { return organization; }
    public String getReason() { return reason; }
    public Status getStatus() { return status; }
    public String getAdminNote() { return adminNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public User getDecidedBy() { return decidedBy; }
}
