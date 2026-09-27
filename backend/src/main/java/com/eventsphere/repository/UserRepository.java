package com.eventsphere.repository;

import com.eventsphere.entity.Role;
import com.eventsphere.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(Role role);

    List<User> findAllByOrderByCreatedAtDesc();

    List<User> findByRoleAndActiveTrue(Role role);

    List<User> findByEmailEndingWithIgnoreCase(String suffix);
}
