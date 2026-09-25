package com.eventsphere.service;

import com.eventsphere.dto.AuthDtos.UserDto;
import com.eventsphere.entity.Role;
import com.eventsphere.entity.User;
import com.eventsphere.exception.BusinessRuleException;
import com.eventsphere.exception.NotFoundException;
import com.eventsphere.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAdminService {

    private final UserRepository userRepository;

    public UserAdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream().map(UserDto::from).toList();
    }

    @Transactional
    public UserDto changeRole(Long userId, Role role, User admin) {
        User user = find(userId);
        if (user.getId().equals(admin.getId())) {
            throw new BusinessRuleException("You cannot change your own role");
        }
        if (user.getRole() == Role.ADMIN && role != Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new BusinessRuleException("The platform needs at least one admin");
        }
        user.setRole(role);
        return UserDto.from(user);
    }

    @Transactional
    public UserDto changeStatus(Long userId, boolean active, User admin) {
        User user = find(userId);
        if (user.getId().equals(admin.getId())) {
            throw new BusinessRuleException("You cannot deactivate your own account");
        }
        user.setActive(active);
        return UserDto.from(user);
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }
}
