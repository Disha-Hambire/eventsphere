# Database walkthrough (for the interview demo)

Open **MySQL Workbench** → connect to `localhost:3306` as `root` → double-click the **`eventsphere`** schema.
Run each query with **Ctrl+Enter**. Every query below proves one design decision.

> **Day before the interview:** run `DROP DATABASE eventsphere;` and restart the backend
> (`powershell -ExecutionPolicy Bypass -File .\run-backend.ps1`). The tables and demo data are recreated with dates
> relative to that moment, so the "live" workshop is really live during your demo.

---

## 1. The schema is versioned, not hand-made

```sql
SELECT version, description, installed_on, success
FROM flyway_schema_history ORDER BY installed_rank;
```
**Say:** "Every change to the database is a numbered migration file (V1-V4) that Flyway applies automatically,
so every environment - my laptop, Docker, the cloud - has exactly the same schema."

## 2. The tables mirror the business process

```sql
SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = 'eventsphere' ORDER BY table_name;
```
**Say:** "users → events → event_sessions (with speakers) → registrations → attendance per session → feedback.
Plus organizer_requests for the approval workflow and password_reset_tokens for secure resets."

## 3. The waitlist is a real FIFO queue

```sql
SELECT u.full_name, r.status, r.registered_at, r.promoted_from_waitlist, r.ticket_code
FROM registrations r
JOIN users u  ON u.id = r.participant_id
JOIN events e ON e.id = r.event_id
WHERE e.title = 'Campus Hackathon 2026'
ORDER BY FIELD(r.status, 'CONFIRMED', 'WAITLISTED', 'CANCELLED'), r.registered_at;
```
**Say:** "Capacity is 5. Confirmed seats first, then the waitlist ordered by registration time. When someone cancels,
the earliest waitlisted person is promoted and `promoted_from_waitlist` becomes 1."
*(Live demo idea: cancel a seat in the app, re-run this query, show the promotion.)*

## 4. Attendance is recorded per session, and it drives the report

```sql
SELECT s.title AS session, COUNT(a.id) AS checked_in
FROM event_sessions s
JOIN events e ON e.id = s.event_id
LEFT JOIN attendance a ON a.session_id = s.id
WHERE e.title = 'TechNova Summit 2026'
GROUP BY s.id, s.title, s.start_time
ORDER BY s.start_time;
```
**Say:** "This is exactly the 'session popularity' chart in the analytics tab - the UI is just a view of this data."

## 5. Feedback only comes from people who actually attended

```sql
SELECT u.full_name, f.rating,
       (SELECT COUNT(*) FROM attendance a
        JOIN registrations r ON r.id = a.registration_id
        WHERE r.event_id = f.event_id AND r.participant_id = f.participant_id) AS sessions_attended
FROM feedback f
JOIN users u  ON u.id = f.participant_id
JOIN events e ON e.id = f.event_id
WHERE e.title = 'TechNova Summit 2026';
```
**Say:** "Every row has sessions_attended ≥ 1 - the service rejects feedback from no-shows, so ratings are trustworthy."

## 6. Secrets are never stored in plain text

```sql
SELECT email, role, active, LEFT(password_hash, 7) AS hash_prefix FROM users LIMIT 5;
SELECT user_id, LEFT(token_hash, 7) AS code_hash, attempts, expires_at, used_at
FROM password_reset_tokens ORDER BY created_at DESC LIMIT 5;
```
**Say:** "Passwords are BCrypt hashes (`$2a$10$`). Even the 6-digit reset codes are hashed, expire in 10 minutes,
and `attempts` locks them after 5 wrong guesses."

## 7. The database enforces the rules too (not only the Java code)

```sql
SELECT table_name, constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_schema = 'eventsphere' AND constraint_type IN ('UNIQUE', 'CHECK')
ORDER BY table_name, constraint_type;
```
**Say:** "`uk_registration_event_participant` means one registration per person per event; `uk_attendance_registration_session`
means nobody is checked into the same session twice; CHECK constraints keep statuses and ratings 1-5 valid.
Even a bug in the code can't break these."

Try it live (it fails, which is the point):
```sql
INSERT INTO feedback (event_id, participant_id, rating, content_rating, organization_rating, would_recommend, submitted_at)
VALUES (1, 1, 9, 5, 5, 1, NOW());   -- rejected: rating must be 1-5
```

## 8. Organizer approval workflow

```sql
SELECT u.full_name, o.organization, o.status, o.created_at, o.decided_at, o.admin_note
FROM organizer_requests o JOIN users u ON u.id = o.user_id
ORDER BY o.created_at DESC;
```
**Say:** "Nobody can publish events without an admin approving them; every decision is recorded with who and when."

---

### If they ask "why MySQL / why this design?"
- **Relational data with strong rules** (capacity, uniqueness, foreign keys) fits a relational database.
- **Row lock** (`SELECT … FOR UPDATE` on the event) during registration, so two people can never take the last seat.
- **Indexes** on `(event_id, status, registered_at)` make the waitlist and counts fast.
- **Grouped queries** for dashboards: 9 queries instead of 46 after measuring it on the cloud deployment.
