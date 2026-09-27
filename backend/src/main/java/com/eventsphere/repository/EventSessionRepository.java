package com.eventsphere.repository;

import com.eventsphere.entity.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface EventSessionRepository extends JpaRepository<EventSession, Long> {

    List<EventSession> findByEventIdOrderByStartTimeAsc(Long eventId);

    long countByEventId(Long eventId);

    long countBySpeakerId(Long speakerId);

    /** Rows of [eventId, sessionCount] for many events in one query. */
    @Query("select s.event.id, count(s) from EventSession s where s.event.id in :eventIds group by s.event.id")
    List<Object[]> countByEventIds(@Param("eventIds") Collection<Long> eventIds);

    /** R9: sessions of the same speaker that overlap [start, end) in any active (draft/published) event. */
    @Query("""
            select s from EventSession s
            where s.speaker.id = :speakerId
              and s.event.status in (com.eventsphere.entity.EventStatus.DRAFT, com.eventsphere.entity.EventStatus.PUBLISHED)
              and s.startTime < :end and s.endTime > :start
              and (:excludeId is null or s.id <> :excludeId)
            """)
    List<EventSession> findSpeakerClashes(@Param("speakerId") Long speakerId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("excludeId") Long excludeId);

    /** R10: sessions of the same event in the same room that overlap [start, end). */
    @Query("""
            select s from EventSession s
            where s.event.id = :eventId
              and lower(s.room) = lower(:room)
              and s.startTime < :end and s.endTime > :start
              and (:excludeId is null or s.id <> :excludeId)
            """)
    List<EventSession> findRoomClashes(@Param("eventId") Long eventId,
                                       @Param("room") String room,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("excludeId") Long excludeId);
}
