package com.eventsphere.service;

import com.eventsphere.dto.FeedbackDtos.FeedbackDto;
import com.eventsphere.dto.FeedbackDtos.FeedbackRequest;
import com.eventsphere.entity.*;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.exception.ConflictException;
import com.eventsphere.repository.AttendanceRepository;
import com.eventsphere.repository.FeedbackRepository;
import com.eventsphere.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Attendance-verified feedback (R19, R20): only people who actually attended can rate the event.
 */
@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final EventService eventService;
    private final AccessPolicy access;
    private final Clock clock;

    public FeedbackService(FeedbackRepository feedbackRepository, RegistrationRepository registrationRepository,
                           AttendanceRepository attendanceRepository, EventService eventService,
                           AccessPolicy access, Clock clock) {
        this.feedbackRepository = feedbackRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.eventService = eventService;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public FeedbackDto submit(Long eventId, FeedbackRequest req, User participant) {
        Event event = eventService.find(eventId);
        // R19
        if (event.getStatus() != EventStatus.COMPLETED) {
            throw new BusinessRuleException("Feedback opens once the organizer marks the event as completed");
        }
        // R20
        Registration registration = registrationRepository.findByEventIdAndParticipantId(eventId, participant.getId())
                .filter(r -> r.getStatus() == RegistrationStatus.CONFIRMED)
                .orElseThrow(() -> new BusinessRuleException("Only confirmed participants of this event can give feedback"));
        if (!attendanceRepository.existsByRegistrationId(registration.getId())) {
            throw new BusinessRuleException("Feedback is only open to participants who attended at least one session");
        }
        if (feedbackRepository.existsByEventIdAndParticipantId(eventId, participant.getId())) {
            throw new ConflictException("You have already shared feedback for this event. Thank you!");
        }
        String comments = req.comments() == null || req.comments().isBlank() ? null : req.comments().trim();
        Feedback feedback = feedbackRepository.save(new Feedback(event, participant, req.rating(), req.contentRating(),
                req.organizationRating(), req.wouldRecommend(), comments, LocalDateTime.now(clock)));
        // The cached AI summary no longer reflects all feedback
        event.setAiFeedbackSummary(null, null);
        return FeedbackDto.from(feedback);
    }

    @Transactional(readOnly = true)
    public List<FeedbackDto> forEvent(Long eventId, User user) {
        Event event = eventService.find(eventId);
        access.requireManage(event, user);
        return feedbackRepository.findByEventIdOrderBySubmittedAtDesc(eventId).stream().map(FeedbackDto::from).toList();
    }
}
