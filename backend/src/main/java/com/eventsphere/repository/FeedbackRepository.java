package com.eventsphere.repository;

import com.eventsphere.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    boolean existsByEventIdAndParticipantId(Long eventId, Long participantId);

    Optional<Feedback> findByEventIdAndParticipantId(Long eventId, Long participantId);

    List<Feedback> findByEventIdOrderBySubmittedAtDesc(Long eventId);

    long countByEventId(Long eventId);

    @Query("select avg(f.rating) from Feedback f")
    Double averageRating();
}
