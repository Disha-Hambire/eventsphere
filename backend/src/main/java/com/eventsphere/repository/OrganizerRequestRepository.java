package com.eventsphere.repository;

import com.eventsphere.entity.OrganizerRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizerRequestRepository extends JpaRepository<OrganizerRequest, Long> {

    boolean existsByUserIdAndStatus(Long userId, OrganizerRequest.Status status);

    Optional<OrganizerRequest> findFirstByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "decidedBy"})
    List<OrganizerRequest> findByStatusOrderByCreatedAtAsc(OrganizerRequest.Status status);

    @EntityGraph(attributePaths = {"user", "decidedBy"})
    List<OrganizerRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(OrganizerRequest.Status status);
}
