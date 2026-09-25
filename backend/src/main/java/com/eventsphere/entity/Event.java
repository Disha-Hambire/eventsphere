package com.eventsphere.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EventCategory category;

    @Column(nullable = false, length = 200)
    private String venue;

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    @Column(name = "registration_deadline", nullable = false)
    private LocalDateTime registrationDeadline;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    /** The organizer who owns (and is accountable for) this event. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id")
    private User organizer;

    /** Cached AI summary of participant feedback (regenerated on demand). */
    @Column(name = "ai_feedback_summary", length = 6000)
    private String aiFeedbackSummary;

    @Column(name = "ai_summary_generated_at")
    private LocalDateTime aiSummaryGeneratedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startTime ASC")
    private List<EventSession> sessions = new ArrayList<>();

    protected Event() {
    }

    public Event(String title, String description, EventCategory category, String venue,
                 LocalDateTime startDateTime, LocalDateTime endDateTime,
                 LocalDateTime registrationDeadline, int capacity, User organizer) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.venue = venue;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.registrationDeadline = registrationDeadline;
        this.capacity = capacity;
        this.organizer = organizer;
    }

    /** Registration is open only for published events before the deadline. */
    public boolean isRegistrationOpen(LocalDateTime now) {
        return status == EventStatus.PUBLISHED && now.isBefore(registrationDeadline);
    }

    public boolean hasStarted(LocalDateTime now) {
        return !now.isBefore(startDateTime);
    }

    /** Completed and cancelled events are locked for changes. */
    public boolean isClosed() {
        return status == EventStatus.COMPLETED || status == EventStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public EventCategory getCategory() { return category; }
    public void setCategory(EventCategory category) { this.category = category; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }
    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(LocalDateTime registrationDeadline) { this.registrationDeadline = registrationDeadline; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public User getOrganizer() { return organizer; }
    public String getAiFeedbackSummary() { return aiFeedbackSummary; }
    public LocalDateTime getAiSummaryGeneratedAt() { return aiSummaryGeneratedAt; }

    public void setAiFeedbackSummary(String summary, LocalDateTime generatedAt) {
        this.aiFeedbackSummary = summary;
        this.aiSummaryGeneratedAt = generatedAt;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<EventSession> getSessions() { return sessions; }
}
