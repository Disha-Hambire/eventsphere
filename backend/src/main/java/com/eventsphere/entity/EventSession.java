package com.eventsphere.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A talk / workshop slot inside an event. Named EventSession to avoid confusion with HTTP sessions.
 */
@Entity
@Table(name = "event_sessions")
public class EventSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speaker_id")
    private Speaker speaker;

    @Column(length = 100)
    private String room;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    protected EventSession() {
    }

    public EventSession(Event event, String title, String description, Speaker speaker,
                        String room, LocalDateTime startTime, LocalDateTime endTime) {
        this.event = event;
        this.title = title;
        this.description = description;
        this.speaker = speaker;
        this.room = room;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return startTime.isBefore(otherEnd) && otherStart.isBefore(endTime);
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Speaker getSpeaker() { return speaker; }
    public void setSpeaker(Speaker speaker) { this.speaker = speaker; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
