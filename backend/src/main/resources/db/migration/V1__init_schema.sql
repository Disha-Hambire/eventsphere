-- =====================================================================
-- EventSphere schema (MySQL 8). Managed by Flyway - do not edit after it
-- has been applied; add a new V2__*.sql migration instead.
-- =====================================================================

CREATE TABLE users (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    full_name       VARCHAR(120) NOT NULL,
    email           VARCHAR(160) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    phone           VARCHAR(30),
    organization    VARCHAR(160),
    role            VARCHAR(20)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'ORGANIZER', 'PARTICIPANT'))
);

CREATE TABLE speakers (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    full_name       VARCHAR(120) NOT NULL,
    email           VARCHAR(160) NOT NULL,
    organization    VARCHAR(160),
    designation     VARCHAR(120),
    bio             VARCHAR(2000),
    expertise       VARCHAR(300),
    PRIMARY KEY (id),
    CONSTRAINT uk_speakers_email UNIQUE (email)
);

CREATE TABLE events (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    title                   VARCHAR(200)  NOT NULL,
    description             VARCHAR(5000),
    category                VARCHAR(20)   NOT NULL,
    venue                   VARCHAR(200)  NOT NULL,
    start_date_time         DATETIME(6)   NOT NULL,
    end_date_time           DATETIME(6)   NOT NULL,
    registration_deadline   DATETIME(6)   NOT NULL,
    capacity                INT           NOT NULL,
    status                  VARCHAR(20)   NOT NULL,
    organizer_id            BIGINT        NOT NULL,
    ai_feedback_summary     VARCHAR(6000),
    ai_summary_generated_at DATETIME(6),
    created_at              DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_id) REFERENCES users (id),
    CONSTRAINT ck_events_capacity CHECK (capacity > 0),
    CONSTRAINT ck_events_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'COMPLETED', 'CANCELLED'))
);
CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_start ON events (start_date_time);

CREATE TABLE event_sessions (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    event_id    BIGINT        NOT NULL,
    title       VARCHAR(200)  NOT NULL,
    description VARCHAR(2000),
    speaker_id  BIGINT,
    room        VARCHAR(100),
    start_time  DATETIME(6)   NOT NULL,
    end_time    DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sessions_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_sessions_speaker FOREIGN KEY (speaker_id) REFERENCES speakers (id)
);
CREATE INDEX idx_sessions_speaker_time ON event_sessions (speaker_id, start_time);

CREATE TABLE registrations (
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    event_id               BIGINT      NOT NULL,
    participant_id         BIGINT      NOT NULL,
    status                 VARCHAR(20) NOT NULL,
    ticket_code            VARCHAR(40) NOT NULL,
    registered_at          DATETIME(6) NOT NULL,
    confirmed_at           DATETIME(6),
    cancelled_at           DATETIME(6),
    promoted_from_waitlist BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_registration_event_participant UNIQUE (event_id, participant_id),
    CONSTRAINT uk_registration_ticket UNIQUE (ticket_code),
    CONSTRAINT fk_registrations_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_registrations_participant FOREIGN KEY (participant_id) REFERENCES users (id),
    CONSTRAINT ck_registrations_status CHECK (status IN ('CONFIRMED', 'WAITLISTED', 'CANCELLED'))
);
CREATE INDEX idx_registrations_event_status ON registrations (event_id, status, registered_at);

CREATE TABLE attendance (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    registration_id BIGINT      NOT NULL,
    session_id      BIGINT      NOT NULL,
    checked_in_at   DATETIME(6) NOT NULL,
    method          VARCHAR(10) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_attendance_registration_session UNIQUE (registration_id, session_id),
    CONSTRAINT fk_attendance_registration FOREIGN KEY (registration_id) REFERENCES registrations (id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_session FOREIGN KEY (session_id) REFERENCES event_sessions (id) ON DELETE CASCADE
);

CREATE TABLE feedback (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    event_id            BIGINT        NOT NULL,
    participant_id      BIGINT        NOT NULL,
    rating              INT           NOT NULL,
    content_rating      INT           NOT NULL,
    organization_rating INT           NOT NULL,
    would_recommend     BOOLEAN       NOT NULL,
    comments            VARCHAR(2000),
    submitted_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_feedback_event_participant UNIQUE (event_id, participant_id),
    CONSTRAINT fk_feedback_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_participant FOREIGN KEY (participant_id) REFERENCES users (id),
    CONSTRAINT ck_feedback_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_feedback_content CHECK (content_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_feedback_org CHECK (organization_rating BETWEEN 1 AND 5)
);
