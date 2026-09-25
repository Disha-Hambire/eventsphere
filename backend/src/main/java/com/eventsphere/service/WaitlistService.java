package com.eventsphere.service;

import com.eventsphere.entity.Event;
import com.eventsphere.entity.Registration;
import com.eventsphere.entity.RegistrationStatus;
import com.eventsphere.repository.RegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The waitlist engine (R4, R15, R16): whenever seats become free, promote waitlisted
 * participants strictly first-come-first-served until the event is full again.
 * Callers must hold the event row lock (EventRepository#findByIdForUpdate).
 */
@Service
public class WaitlistService {

    private static final Logger log = LoggerFactory.getLogger(WaitlistService.class);

    private final RegistrationRepository registrationRepository;

    public WaitlistService(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    public List<Registration> fillOpenSeats(Event event, LocalDateTime now) {
        List<Registration> promoted = new ArrayList<>();
        // Seats are only reallocated while the event is still open and has not started.
        if (event.isClosed() || event.hasStarted(now)) {
            return promoted;
        }
        long confirmed = registrationRepository.countByEventIdAndStatus(event.getId(), RegistrationStatus.CONFIRMED);
        long freeSeats = event.getCapacity() - confirmed;
        if (freeSeats <= 0) {
            return promoted;
        }
        List<Registration> queue = registrationRepository
                .findByEventIdAndStatusOrderByRegisteredAtAsc(event.getId(), RegistrationStatus.WAITLISTED);
        for (Registration next : queue) {
            if (freeSeats-- <= 0) {
                break;
            }
            next.promoteFromWaitlist(now);
            promoted.add(next);
            log.info("Promoted registration {} ({}) from waitlist for event {}",
                    next.getId(), next.getParticipant().getEmail(), event.getId());
        }
        return promoted;
    }

    public Integer waitlistPosition(Registration r) {
        if (r.getStatus() != RegistrationStatus.WAITLISTED) {
            return null;
        }
        return (int) registrationRepository.countByEventIdAndStatusAndRegisteredAtBefore(
                r.getEvent().getId(), RegistrationStatus.WAITLISTED, r.getRegisteredAt()) + 1;
    }
}
