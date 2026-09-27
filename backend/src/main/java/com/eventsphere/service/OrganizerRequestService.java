package com.eventsphere.service;

import com.eventsphere.dto.OrganizerRequestDtos.OrganizerRequestDto;
import com.eventsphere.entity.OrganizerRequest;
import com.eventsphere.entity.OrganizerRequest.Status;
import com.eventsphere.entity.Role;
import com.eventsphere.entity.User;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.exception.ConflictException;
import com.eventsphere.exception.NotFoundException;
import com.eventsphere.repository.OrganizerRequestRepository;
import com.eventsphere.repository.UserRepository;
import com.eventsphere.service.mail.Notifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Becoming an organizer: a participant applies with a reason, an admin approves (role becomes ORGANIZER) or rejects.
 * Rules: only participants can apply; one pending request at a time; a rejected participant may apply again.
 */
@Service
public class OrganizerRequestService {

    private final OrganizerRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final Notifier notifier;
    private final Clock clock;

    public OrganizerRequestService(OrganizerRequestRepository requestRepository, UserRepository userRepository,
                                   Notifier notifier, Clock clock) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.notifier = notifier;
        this.clock = clock;
    }

    @Transactional
    public OrganizerRequestDto submit(User user, String organization, String reason) {
        if (user.getRole() != Role.PARTICIPANT) {
            throw new BusinessRuleException("You already have " + user.getRole().name().toLowerCase() + " access");
        }
        if (requestRepository.existsByUserIdAndStatus(user.getId(), Status.PENDING)) {
            throw new ConflictException("You already have a request waiting for an admin's decision");
        }
        String org = organization == null || organization.isBlank() ? user.getOrganization() : organization.trim();
        OrganizerRequest request = requestRepository.save(
                new OrganizerRequest(user, org, reason.trim(), LocalDateTime.now(clock)));

        for (User admin : userRepository.findByRoleAndActiveTrue(Role.ADMIN)) {
            notifier.send(admin.getEmail(), admin.getFullName(),
                    "New organizer request from " + user.getFullName(),
                    user.getFullName() + " (" + user.getEmail() + ")" + (org == null ? "" : " from " + org)
                            + " asked to become an organizer:\n\n\"" + request.getReason() + "\"\n\n"
                            + "Review it in EventSphere under Users & roles.\n\n- EventSphere");
        }
        return OrganizerRequestDto.from(request);
    }

    /** The caller's most recent request, or null if they never applied. */
    @Transactional(readOnly = true)
    public OrganizerRequestDto latestFor(User user) {
        return requestRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(user.getId())
                .map(OrganizerRequestDto::from)
                .orElse(null);
    }

    /** Pending requests (oldest first), or all requests newest first when status is null. */
    @Transactional(readOnly = true)
    public List<OrganizerRequestDto> list(Status status) {
        List<OrganizerRequest> rows = status == null
                ? requestRepository.findAllByOrderByCreatedAtDesc()
                : requestRepository.findByStatusOrderByCreatedAtAsc(status);
        return rows.stream().map(OrganizerRequestDto::from).toList();
    }

    @Transactional
    public OrganizerRequestDto approve(Long id, User admin, String note) {
        OrganizerRequest request = pending(id);
        User applicant = request.getUser();
        if (applicant.getRole() == Role.PARTICIPANT) {
            applicant.setRole(Role.ORGANIZER);
        }
        request.decide(Status.APPROVED, admin, blankToNull(note), LocalDateTime.now(clock));
        notifier.send(applicant.getEmail(), applicant.getFullName(), "You're now an EventSphere organizer",
                "Hi " + applicant.getFullName() + ",\n\nGood news: your request to become an organizer was approved."
                        + (request.getAdminNote() == null ? "" : "\n\nNote from the admin: " + request.getAdminNote())
                        + "\n\nOpen EventSphere to create your first event.\n\n- EventSphere");
        return OrganizerRequestDto.from(request);
    }

    @Transactional
    public OrganizerRequestDto reject(Long id, User admin, String note) {
        OrganizerRequest request = pending(id);
        User applicant = request.getUser();
        request.decide(Status.REJECTED, admin, blankToNull(note), LocalDateTime.now(clock));
        notifier.send(applicant.getEmail(), applicant.getFullName(), "About your EventSphere organizer request",
                "Hi " + applicant.getFullName() + ",\n\nYour request to become an organizer was not approved this time."
                        + (request.getAdminNote() == null ? "" : "\n\nNote from the admin: " + request.getAdminNote())
                        + "\n\nYou're welcome to apply again with more details.\n\n- EventSphere");
        return OrganizerRequestDto.from(request);
    }

    public long pendingCount() {
        return requestRepository.countByStatus(Status.PENDING);
    }

    private OrganizerRequest pending(Long id) {
        OrganizerRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organizer request", id));
        if (request.getStatus() != Status.PENDING) {
            throw new BusinessRuleException("This request was already " + request.getStatus().name().toLowerCase());
        }
        return request;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
