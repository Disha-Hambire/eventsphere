package com.eventsphere.service;

import com.eventsphere.entity.Event;
import com.eventsphere.entity.Role;
import com.eventsphere.entity.User;
import com.eventsphere.exception.ForbiddenException;
import org.springframework.stereotype.Component;

/**
 * Ownership rules: an ADMIN can manage every event, an ORGANIZER only the events they own.
 * (Role checks such as "only organizers can create events" are done with @PreAuthorize on the controllers.)
 */
@Component
public class AccessPolicy {

    public boolean canManage(Event event, User user) {
        if (user == null) {
            return false;
        }
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return user.getRole() == Role.ORGANIZER && event.getOrganizer().getId().equals(user.getId());
    }

    public void requireManage(Event event, User user) {
        if (!canManage(event, user)) {
            throw new ForbiddenException("Only the organizer of '" + event.getTitle() + "' or an admin can do this");
        }
    }

    public boolean isParticipant(User user) {
        return user != null && user.getRole() == Role.PARTICIPANT;
    }
}
