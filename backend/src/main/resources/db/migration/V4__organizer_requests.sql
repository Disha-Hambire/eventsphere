-- Participants apply to become organizers; an admin approves or rejects.
CREATE TABLE organizer_requests (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,
    organization  VARCHAR(160),
    reason        VARCHAR(1000) NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    admin_note    VARCHAR(500),
    created_at    DATETIME(6)   NOT NULL,
    decided_at    DATETIME(6),
    decided_by    BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_orgreq_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_orgreq_decided_by FOREIGN KEY (decided_by) REFERENCES users (id),
    CONSTRAINT ck_orgreq_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);
CREATE INDEX idx_orgreq_status_created ON organizer_requests (status, created_at);
CREATE INDEX idx_orgreq_user ON organizer_requests (user_id);
