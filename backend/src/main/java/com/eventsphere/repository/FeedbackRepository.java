package com.eventsphere.repository;

import com.eventsphere.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    boolean existsByEventIdAndParticipantId(Long eventId, Long participantId);

    Optional<Feedback> findByEventIdAndParticipantId(Long eventId, Long participantId);

    List<Feedback> findByEventIdOrderBySubmittedAtDesc(Long eventId);

    long countByEventId(Long eventId);

    @Query("select f.rating from Feedback f where f.event.id in :eventIds")
    List<Integer> findRatingsByEventIds(@Param("eventIds") Collection<Long> eventIds);

    @Query("select avg(f.rating) from Feedback f")
    Double averageRating();
}
