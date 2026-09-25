package com.eventsphere.service;

import com.eventsphere.dto.SpeakerDtos.SpeakerDto;
import com.eventsphere.dto.SpeakerDtos.SpeakerRequest;
import com.eventsphere.entity.Speaker;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.exception.ConflictException;
import com.eventsphere.exception.NotFoundException;
import com.eventsphere.repository.EventSessionRepository;
import com.eventsphere.repository.SpeakerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SpeakerService {

    private final SpeakerRepository speakerRepository;
    private final EventSessionRepository sessionRepository;

    public SpeakerService(SpeakerRepository speakerRepository, EventSessionRepository sessionRepository) {
        this.speakerRepository = speakerRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public List<SpeakerDto> list() {
        return speakerRepository.findAllByOrderByFullNameAsc().stream()
                .map(s -> SpeakerDto.from(s, sessionRepository.countBySpeakerId(s.getId())))
                .toList();
    }

    @Transactional
    public SpeakerDto create(SpeakerRequest req) {
        String email = req.email().trim().toLowerCase();
        if (speakerRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A speaker with email " + email + " already exists");
        }
        Speaker s = new Speaker(req.fullName().trim(), email, req.organization(), req.designation(),
                req.bio(), req.expertise());
        return SpeakerDto.from(speakerRepository.save(s), 0);
    }

    @Transactional
    public SpeakerDto update(Long id, SpeakerRequest req) {
        Speaker s = find(id);
        String email = req.email().trim().toLowerCase();
        if (speakerRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("A speaker with email " + email + " already exists");
        }
        s.setFullName(req.fullName().trim());
        s.setEmail(email);
        s.setOrganization(req.organization());
        s.setDesignation(req.designation());
        s.setBio(req.bio());
        s.setExpertise(req.expertise());
        return SpeakerDto.from(s, sessionRepository.countBySpeakerId(id));
    }

    /** R11: a speaker who is assigned to sessions cannot be deleted (history must stay intact). */
    @Transactional
    public void delete(Long id) {
        Speaker s = find(id);
        long sessions = sessionRepository.countBySpeakerId(id);
        if (sessions > 0) {
            throw new BusinessRuleException(s.getFullName() + " is assigned to " + sessions
                    + " session(s). Reassign or remove those sessions first.");
        }
        speakerRepository.delete(s);
    }

    Speaker find(Long id) {
        return speakerRepository.findById(id).orElseThrow(() -> new NotFoundException("Speaker", id));
    }
}
