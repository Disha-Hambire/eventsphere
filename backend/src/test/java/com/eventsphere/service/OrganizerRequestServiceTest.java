package com.eventsphere.service;

import com.eventsphere.dto.OrganizerRequestDtos.OrganizerRequestDto;
import com.eventsphere.entity.OrganizerRequest.Status;
import com.eventsphere.entity.Role;
import com.eventsphere.entity.User;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.exception.ConflictException;
import com.eventsphere.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizerRequestServiceTest extends IntegrationTest {

    @Autowired OrganizerRequestService service;

    private static final String REASON = "I run the college tech club and organise monthly workshops.";

    @Test
    @DisplayName("A participant applies, the admin approves, and the participant becomes an organizer")
    void approveMakesOrganizer() {
        User participant = user(Role.PARTICIPANT);
        User admin = user(Role.ADMIN);

        OrganizerRequestDto request = service.submit(participant, "Tech Club", REASON);
        assertThat(request.status()).isEqualTo(Status.PENDING);
        assertThat(service.list(Status.PENDING)).extracting(OrganizerRequestDto::id).contains(request.id());

        OrganizerRequestDto approved = service.approve(request.id(), admin, "Welcome aboard");
        assertThat(approved.status()).isEqualTo(Status.APPROVED);
        assertThat(approved.decidedByName()).isEqualTo(admin.getFullName());
        assertThat(userRepository.findById(participant.getId()).orElseThrow().getRole()).isEqualTo(Role.ORGANIZER);
    }

    @Test
    @DisplayName("Only one pending request at a time; after a rejection the participant may apply again")
    void onePendingAtATime() {
        User participant = user(Role.PARTICIPANT);
        User admin = user(Role.ADMIN);
        OrganizerRequestDto first = service.submit(participant, null, REASON);

        assertThatThrownBy(() -> service.submit(participant, null, REASON))
                .isInstanceOf(ConflictException.class);

        service.reject(first.id(), admin, "Please add your college name");
        assertThat(userRepository.findById(participant.getId()).orElseThrow().getRole()).isEqualTo(Role.PARTICIPANT);
        assertThat(service.latestFor(participant).adminNote()).isEqualTo("Please add your college name");

        OrganizerRequestDto second = service.submit(participant, "PIT College", REASON);
        assertThat(second.status()).isEqualTo(Status.PENDING);
        assertThat(service.latestFor(participant).id()).isEqualTo(second.id());
    }

    @Test
    @DisplayName("Organizers and admins cannot apply; a decided request cannot be decided again")
    void guards() {
        assertThatThrownBy(() -> service.submit(user(Role.ORGANIZER), null, REASON))
                .isInstanceOf(BusinessRuleException.class);

        User admin = user(Role.ADMIN);
        OrganizerRequestDto request = service.submit(user(Role.PARTICIPANT), null, REASON);
        service.approve(request.id(), admin, null);
        assertThatThrownBy(() -> service.reject(request.id(), admin, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already approved");
    }
}
