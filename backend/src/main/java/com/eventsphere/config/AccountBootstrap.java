package com.eventsphere.config;

import com.eventsphere.entity.Role;
import com.eventsphere.entity.User;
import com.eventsphere.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Production account setup, driven by configuration so no password is ever handled:
 * <ul>
 *   <li>ADMIN_EMAILS: these accounts become (and stay) active admins, at startup and when they sign up.</li>
 *   <li>DISABLE_DEMO_ACCOUNTS=true: the seeded demo accounts (whose passwords are public in the README)
 *       are deactivated, so nobody can log in with them. Their events stay. An admin can reactivate them.</li>
 * </ul>
 * Runs after {@link DataSeeder}.
 */
@Component
@Order(2)
public class AccountBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountBootstrap.class);

    /** E-mail domains used only by the demo data set. */
    static final List<String> DEMO_DOMAINS = List.of("@eventsphere.com", "@example.com");

    private final Set<String> adminEmails;
    private final boolean disableDemoAccounts;
    private final UserRepository users;
    private final TransactionTemplate tx;

    public AccountBootstrap(@Value("${app.admin-emails:}") String adminEmails,
                            @Value("${app.demo.disable-accounts:false}") boolean disableDemoAccounts,
                            UserRepository users, TransactionTemplate tx) {
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.disableDemoAccounts = disableDemoAccounts;
        this.users = users;
        this.tx = tx;
    }

    public boolean isAdminEmail(String email) {
        return email != null && adminEmails.contains(email.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    public void run(String... args) {
        tx.executeWithoutResult(status -> apply());
    }

    void apply() {
        for (String email : adminEmails) {
            users.findByEmailIgnoreCase(email).ifPresentOrElse(u -> {
                if (u.getRole() != Role.ADMIN || !u.isActive()) {
                    u.setRole(Role.ADMIN);
                    u.setActive(true);
                    log.info("Granted admin access to {} (ADMIN_EMAILS)", email);
                }
            }, () -> log.info("ADMIN_EMAILS: {} has no account yet; it becomes admin when it signs up", email));
        }
        if (disableDemoAccounts) {
            int count = 0;
            for (String domain : DEMO_DOMAINS) {
                for (User u : users.findByEmailEndingWithIgnoreCase(domain)) {
                    if (u.isActive() && !isAdminEmail(u.getEmail())) {
                        u.setActive(false);
                        count++;
                    }
                }
            }
            if (count > 0) {
                log.info("Deactivated {} demo account(s) (DISABLE_DEMO_ACCOUNTS=true)", count);
            }
        }
    }
}
