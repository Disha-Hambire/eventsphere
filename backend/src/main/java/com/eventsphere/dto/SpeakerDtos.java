package com.eventsphere.dto;

import com.eventsphere.entity.Speaker;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class SpeakerDtos {

    private SpeakerDtos() {
    }

    public record SpeakerRequest(
            @NotBlank(message = "Speaker name is required") @Size(max = 120) String fullName,
            @NotBlank(message = "Speaker email is required") @Email(message = "Enter a valid email") String email,
            @Size(max = 160) String organization,
            @Size(max = 120) String designation,
            @Size(max = 2000) String bio,
            @Size(max = 300) String expertise) {
    }

    public record SpeakerDto(Long id, String fullName, String email, String organization,
                             String designation, String bio, String expertise, long sessionCount) {

        public static SpeakerDto from(Speaker s, long sessionCount) {
            return new SpeakerDto(s.getId(), s.getFullName(), s.getEmail(), s.getOrganization(),
                    s.getDesignation(), s.getBio(), s.getExpertise(), sessionCount);
        }
    }
}
