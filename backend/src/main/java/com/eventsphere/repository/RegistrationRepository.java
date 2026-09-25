package com.eventsphere.repository;

import com.eventsphere.entity.Registration;
import com.eventsphere.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    Optional<Registration> findByEventIdAndParticipantId(Long eventId, Long participantId);

    Optional<Registration> findByTicketCode(String ticketCode);

    boolean existsByTicketCode(String ticketCode);

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    long countByStatus(RegistrationStatus status);

    long countByEventIdAndPromotedFromWaitlistTrue(Long eventId);

    /** Waitlist queue in FIFO order. */
    List<Registration> findByEventIdAndStatusOrderByRegisteredAtAsc(Long eventId, RegistrationStatus status);

    List<Registration> findByEventIdOrderByRegisteredAtAsc(Long eventId);

    List<Registration> findByParticipantIdOrderByRegisteredAtDesc(Long participantId);

    /** Position in the waitlist = number of people who joined the waitlist earlier + 1. */
    long countByEventIdAndStatusAndRegisteredAtBefore(Long eventId, RegistrationStatus status, LocalDateTime registeredAt);

    /** Smart registration: the participant's other active registrations whose event overlaps this time window. */
    @Query("""
            select r from Registration r
            where r.participant.id = :participantId
              and r.event.id <> :eventId
              and r.status = com.eventsphere.entity.RegistrationStatus.CONFIRMED
              and r.event.status = com.eventsphere.entity.EventStatus.PUBLISHED
              and r.event.startDateTime < :end and r.event.endDateTime > :start
            """)
    List<Registration> findOverlappingConfirmed(@Param("participantId") Long participantId,
                                                @Param("eventId") Long eventId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    @Query("select r.registeredAt from Registration r where r.registeredAt >= :since")
    List<LocalDateTime> findRegistrationTimesSince(@Param("since") LocalDateTime since);

    @Query("""
            select r.registeredAt from Registration r
            where r.registeredAt >= :since and r.event.organizer.id = :organizerId
            """)
    List<LocalDateTime> findRegistrationTimesSinceForOrganizer(@Param("since") LocalDateTime since,
                                                               @Param("organizerId") Long organizerId);

    @Query("select count(distinct r.participant.id) from Registration r where r.status <> com.eventsphere.entity.RegistrationStatus.CANCELLED")
    long countDistinctActiveParticipants();
}
