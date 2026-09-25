-- Password reset now uses a 6-digit e-mailed code. token_hash holds a BCrypt hash of the code;
-- attempts counts wrong guesses so a code is locked after 5 failures (brute-force protection).
ALTER TABLE password_reset_tokens ADD COLUMN attempts INT NOT NULL DEFAULT 0;
