# Software Requirements Specification: EventSphere

*Structured after IEEE Std 830-1998 / ISO/IEC/IEEE 29148 (Software Requirements Specification).*

| Item | Value |
|---|---|
| Product | EventSphere, an AI-powered event management platform |
| Version | 1.0 |
| Date | September 2026 |
| Status | Implemented |

---

## 1. Introduction

### 1.1 Purpose
This document specifies the functional and non-functional requirements of EventSphere. It is intended for
developers, testers, evaluators and stakeholders who operate college, corporate and conference events.

### 1.2 Scope
EventSphere manages the complete lifecycle of an event: planning the agenda and speakers, publishing,
registration with capacity and waitlist, QR ticketing, session-wise check-in, closing, attendance-verified
feedback, and analytics, including AI-generated descriptions and feedback summaries (Google Gemini).
Out of scope: payments/ticket sales, e-mail/SMS delivery, multi-tenant organisations, native mobile apps.

### 1.3 Definitions, acronyms and abbreviations
| Term | Definition |
|---|---|
| Event | A scheduled gathering with a venue, time window and seat capacity |
| Session | A time slot inside an event (talk, workshop), optionally with a speaker and room |
| Registration | A participant's claim on a seat: CONFIRMED, WAITLISTED or CANCELLED |
| Waitlist | FIFO queue of registrations waiting for a seat |
| Ticket code | Unique random code (`ES-XXXX-XXXX`) encoded in the participant's QR ticket |
| Check-in / attendance | Record that a confirmed participant entered a specific session |
| Phase | Time-derived state of a published event: UPCOMING, LIVE, ENDED |
| JWT | JSON Web Token used for stateless authentication |
| FIFO | First in, first out |

### 1.4 References
- IEEE Std 830-1998, *Recommended Practice for Software Requirements Specifications*
- `docs/BUSINESS-PROCESS.md`: process model and business rules R1-R20
- `docs/API.md`: REST interface

### 1.5 Overview
Section 2 describes the product context; Section 3 lists specific requirements; Section 4 gives traceability to tests.

## 2. Overall description

### 2.1 Product perspective
A standalone web application: React single-page front end → Spring Boot REST API → MySQL database, with an
optional outbound integration to the Google Gemini API.

```
Browser (React 19 SPA) ──HTTPS/JSON──► Spring Boot 3 API ──JDBC──► MySQL 8
                                             └──HTTPS──► Google Gemini (optional)
```

### 2.2 Product functions
F1 Authentication & roles · F2 User administration · F3 Speaker directory · F4 Event lifecycle ·
F5 Agenda scheduling · F6 Smart registration & waitlist · F7 QR tickets · F8 QR check-in & attendance ·
F9 Feedback · F10 Analytics & reports · F11 AI description generation · F12 AI feedback summarisation · F13 Data export.

### 2.3 User classes and characteristics
| Class | Characteristics |
|---|---|
| Admin | Few users; full platform control |
| Organizer | Staff/faculty/event managers; moderate technical skill; uses laptop at desk and phone at the door |
| Participant | Students, employees, public; uses mobile phones; low tolerance for friction |

### 2.4 Operating environment
Any modern browser (Chrome, Edge, Firefox, Safari), desktop or mobile; camera access for scanning.
Server: Java 21 runtime, MySQL 8, 512 MB RAM minimum; Docker supported; deployable to Render.

### 2.5 Design and implementation constraints
Java 21 / Spring Boot 3.5, React 19 / Vite / Tailwind CSS 4, MySQL 8 with Flyway migrations, JWT (HS256),
REST/JSON, OpenAPI 3 documentation.

### 2.6 Assumptions and dependencies
Single organisation; free events; local wall-clock time zone; Gemini is optional and every AI feature has a fallback.

## 3. Specific requirements

### 3.1 External interface requirements
- **User interface:** responsive (≥ 360 px), light and dark themes, WCAG-conscious contrast, keyboard-closable dialogs.
- **Software interface:** REST/JSON under `/api`; OpenAPI at `/v3/api-docs`, Swagger UI at `/swagger-ui.html`.
- **Hardware interface:** device camera via the browser for QR scanning (manual code entry as fallback).
- **Communications:** HTTPS in production; CORS restricted to configured origins.

### 3.2 Functional requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-1 | Visitors shall sign up as participants with name, e-mail and password (≥ 6 chars). | High |
| FR-2 | Users shall log in with e-mail/password and receive a JWT valid for a configurable period. | High |
| FR-2a | Users shall reset a forgotten password with a 6-digit code e-mailed to them (single use, 10-minute expiry, max 5 wrong attempts); the request must not reveal whether an account exists. | Medium |
| FR-3 | The system shall enforce role-based access for ADMIN, ORGANIZER, PARTICIPANT. | High |
| FR-4 | Admins shall change a user's role and activate/deactivate accounts; they cannot demote/deactivate themselves or remove the last admin. | Medium |
| FR-5 | Organizers shall create events (title, description, category, venue, start, end, deadline, capacity) that start as DRAFT. | High |
| FR-6 | The system shall validate event dates (R1) and capacity (R3). | High |
| FR-7 | Organizers shall publish, complete and cancel events according to the state machine (R2, R6, R7). | High |
| FR-8 | Organizers shall manage sessions; the system shall reject sessions outside the event, speaker double-booking and room clashes (R8-R10). | High |
| FR-9 | Organizers shall maintain a speaker directory; assigned speakers cannot be deleted (R11). | Medium |
| FR-10 | Participants shall register for open events; the system shall confirm if seats remain, otherwise waitlist (R12-R14). | High |
| FR-11 | The system shall show each waitlisted participant their FIFO position (R15). | High |
| FR-12 | On cancellation of a confirmed seat or capacity increase, the system shall promote waitlisted participants automatically (R4, R16). | High |
| FR-13 | The system shall prevent duplicate registrations and overlapping confirmed registrations for one participant. | Medium |
| FR-14 | Each confirmed participant shall receive a unique QR ticket. | High |
| FR-15 | Organizers shall check participants into a session by scanning the QR or typing the code; duplicate scans shall be reported, invalid tickets refused with a reason (R17, R18). | High |
| FR-16 | Organizers shall view and edit a per-session attendance sheet. | Medium |
| FR-17 | Participants who attended shall submit one rating after completion (R19, R20). | High |
| FR-18 | The system shall produce per-event reports: fill rate, attendance rate, no-shows, session popularity, rating distribution. | High |
| FR-19 | The system shall provide a dashboard with portfolio KPIs, a 14-day registration trend and category mix. | Medium |
| FR-20 | Organizers shall generate an event description with AI from title, audience and highlights. | Medium |
| FR-21 | Organizers shall generate an AI summary of feedback (sentiment, strengths, improvements, action items). | Medium |
| FR-22 | Organizers shall export registrations as CSV. | Low |
| FR-23 | Participants shall see a timeline of their registration (registered, waitlisted, promoted, checked in, feedback). | Low |

### 3.3 Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-1 | Security | Passwords hashed with BCrypt; stateless JWT; secrets only via environment variables. |
| NFR-2 | Security | Authorisation checked server-side on every request (role + ownership), never only in the UI. |
| NFR-3a | Security | Password-reset codes: SecureRandom, stored only as BCrypt hashes, single use, 5-attempt lockout, older codes invalidated on a new request. |
| NFR-3 | Security | CSV export neutralises spreadsheet formula injection; ticket codes are unguessable (SecureRandom). |
| NFR-4 | Integrity | Seat allocation is serialised with a pessimistic row lock: capacity can never be exceeded under concurrency. |
| NFR-5 | Integrity | Unique constraints on (event, participant), ticket code, (registration, session), (event, participant) feedback. |
| NFR-6 | Reliability | AI calls time out (30 s) and fall back to deterministic generators; the app works without Gemini. |
| NFR-7 | Performance | Typical API responses < 300 ms for demo-scale data; UI first load < 3 s on broadband. |
| NFR-8 | Usability | Every business-rule violation returns a human-readable message shown to the user. |
| NFR-9 | Maintainability | Layered architecture (controller → service → repository); rules centralised in services; schema versioned with Flyway. |
| NFR-10 | Testability | Time injected via `Clock`; business rules covered by automated tests; CI on every push. |
| NFR-11 | Portability | Runs via `docker compose up`; deployable to Render; configuration by environment variables. |
| NFR-12 | Observability | Health endpoint `/actuator/health`; structured server logs for failures and waitlist promotions. |

## 4. Verification (traceability)

| Rule / requirement | Automated test |
|---|---|
| R2 publish needs session (FR-7) | `EventServiceTest.publishRequiresSession` |
| R1 deadline before start (FR-6) | `EventServiceTest.deadlineAfterStartRejected` |
| R3 capacity floor (FR-6) | `EventServiceTest.capacityCannotDropBelowConfirmed` |
| R4 capacity increase promotes (FR-12) | `EventServiceTest.capacityIncreasePromotesWaitlist` |
| R6 only drafts deleted | `EventServiceTest.onlyDraftsCanBeDeleted` |
| R7 complete after start | `EventServiceTest.cannotCompleteBeforeStart` |
| Ownership (NFR-2) | `EventServiceTest.ownershipEnforced`, `draftsHiddenFromParticipants` |
| R8-R10 scheduling (FR-8) | `SessionServiceTest.*` |
| R12-R16 registration & waitlist (FR-10-13), FR-22 CSV | `RegistrationServiceTest.*` |
| R17-R18 check-in (FR-15) | `AttendanceAndFeedbackTest.qrCheckInIsIdempotent`, `checkInWindowEnforced`, `waitlistedCannotCheckIn`, `ticketForOtherEventRejected` |
| R19-R20 feedback (FR-17) | `AttendanceAndFeedbackTest.feedbackRules` |
| FR-21 AI fallback (NFR-6) | `FallbackGeneratorTest.*` |
| FR-1-3, NFR-1-2 | `SecurityApiTest.*` |
| FR-2a password reset | `PasswordResetServiceTest.*` |

Run all: `cd backend && mvn test` (37 tests).
