package com.eventsphere.repository;

import com.eventsphere.entity.Speaker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpeakerRepository extends JpaRepository<Speaker, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Speaker> findAllByOrderByFullNameAsc();
}
