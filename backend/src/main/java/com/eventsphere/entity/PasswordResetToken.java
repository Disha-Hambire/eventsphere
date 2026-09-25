package com.eventsphere.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A single-use, time-limited password reset code sent to the user's e-mail.
 * Only a BCrypt hash of the 6-digit code is stored, and wrong guesses are counted.
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private int attempts;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(User user, String tokenHash, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isLocked(int maxAttempts) {
        return attempts >= maxAttempts;
    }

    public void registerFailedAttempt() {
        attempts++;
    }

    public void markUsed(LocalDateTime now) {
        this.usedAt = now;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public int getAttempts() { return attempts; }
}
