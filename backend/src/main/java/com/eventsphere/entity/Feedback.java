package com.eventsphere.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback",
        uniqueConstraints = @UniqueConstraint(name = "uk_feedback_event_participant",
                columnNames = {"event_id", "participant_id"}))
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id")
    private User participant;

    /** Overall rating 1-5. */
    @Column(nullable = false)
    private int rating;

    /** Quality of content / speakers, 1-5. */
    @Column(name = "content_rating", nullable = false)
    private int contentRating;

    /** How well the event was organised, 1-5. */
    @Column(name = "organization_rating", nullable = false)
    private int organizationRating;

    @Column(name = "would_recommend", nullable = false)
    private boolean wouldRecommend;

    @Column(length = 2000)
    private String comments;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    protected Feedback() {
    }

    public Feedback(Event event, User participant, int rating, int contentRating, int organizationRating,
                    boolean wouldRecommend, String comments, LocalDateTime submittedAt) {
        this.event = event;
        this.participant = participant;
        this.rating = rating;
        this.contentRating = contentRating;
        this.organizationRating = organizationRating;
        this.wouldRecommend = wouldRecommend;
        this.comments = comments;
        this.submittedAt = submittedAt;
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public User getParticipant() { return participant; }
    public int getRating() { return rating; }
    public int getContentRating() { return contentRating; }
    public int getOrganizationRating() { return organizationRating; }
    public boolean isWouldRecommend() { return wouldRecommend; }
    public String getComments() { return comments; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
