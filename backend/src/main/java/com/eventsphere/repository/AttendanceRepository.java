package com.eventsphere.repository;

import com.eventsphere.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByRegistrationIdAndSessionId(Long registrationId, Long sessionId);

    List<Attendance> findBySessionId(Long sessionId);

    List<Attendance> findByRegistrationId(Long registrationId);

    long countBySessionId(Long sessionId);

    long countByRegistrationId(Long registrationId);

    boolean existsByRegistrationId(Long registrationId);

    /** Number of distinct participants who attended at least one session of the event. */
    @Query("select count(distinct a.registration.id) from Attendance a where a.registration.event.id = :eventId")
    long countDistinctAttendeesByEventId(@Param("eventId") Long eventId);

    /** Rows of [eventId, checkIns, distinctAttendees] for many events in one query. */
    @Query("""
            select a.registration.event.id, count(a), count(distinct a.registration.id)
            from Attendance a where a.registration.event.id in :eventIds
            group by a.registration.event.id
            """)
    List<Object[]> statsByEventIds(@Param("eventIds") Collection<Long> eventIds);

    @Query("select count(a) from Attendance a where a.registration.event.id = :eventId")
    long countByEventId(@Param("eventId") Long eventId);
}
