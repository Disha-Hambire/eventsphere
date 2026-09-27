package com.eventsphere.config;

import com.eventsphere.entity.Role;
import com.eventsphere.entity.User;
import com.eventsphere.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AccountBootstrapTest extends IntegrationTest {

    @Autowired TransactionTemplate tx;

    @Test
    @DisplayName("ADMIN_EMAILS promotes the owner; DISABLE_DEMO_ACCOUNTS locks every demo account but not real ones")
    void promotesOwnerAndDisablesDemoAccounts() {
        User owner = userRepository.save(new User("Owner", "owner@gmail.test", "x", Role.PARTICIPANT));
        User demoAdmin = userRepository.save(new User("Demo", "boss@eventsphere.com", "x", Role.ADMIN));
        User demoPerson = userRepository.save(new User("Riya", "riya2@example.com", "x", Role.PARTICIPANT));
        User realPerson = userRepository.save(new User("Real", "real@college.test", "x", Role.PARTICIPANT));

        new AccountBootstrap("Owner@Gmail.test", true, userRepository, tx).apply();

        assertThat(userRepository.findById(owner.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
        assertThat(userRepository.findById(demoAdmin.getId()).orElseThrow().isActive()).isFalse();
        assertThat(userRepository.findById(demoPerson.getId()).orElseThrow().isActive()).isFalse();
        assertThat(userRepository.findById(realPerson.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("Without the switch, demo accounts stay active")
    void demoAccountsUntouchedByDefault() {
        User demo = userRepository.save(new User("Demo", "demo3@example.com", "x", Role.PARTICIPANT));
        new AccountBootstrap("", false, userRepository, tx).apply();
        assertThat(userRepository.findById(demo.getId()).orElseThrow().isActive()).isTrue();
    }
}
