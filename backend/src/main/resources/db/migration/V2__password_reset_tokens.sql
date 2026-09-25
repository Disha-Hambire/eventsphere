-- Single-use password reset tokens. Only a SHA-256 hash of the token is stored,
-- so a database leak cannot be used to reset anyone's password.
CREATE TABLE password_reset_tokens (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  DATETIME(6) NOT NULL,
    used_at     DATETIME(6),
    created_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_password_reset_user ON password_reset_tokens (user_id);
