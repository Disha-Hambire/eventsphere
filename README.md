# EventSphere

**AI-powered event management for colleges, companies and conferences.** One system for the whole lifecycle:
**plan → publish → register → check in → close → feedback → analyse.**

Built for the Thinqloud Campus Application Development Assessment (topic #10, *Event Management Platform*).

| | |
|---|---|
| **Frontend** | React 19, Vite 8, Tailwind CSS 4, Motion, Recharts, QR (qrcode.react + html5-qrcode) |
| **Backend** | Java 21, Spring Boot 3.5, Spring Security (JWT), Spring Data JPA, Flyway, Springdoc (Swagger) |
| **Database** | MySQL 8 (H2 in MySQL mode for tests and zero-install runs) |
| **AI** | Google Gemini (event descriptions, feedback summaries) with rule-based fallback |
| **DevOps** | Docker, docker-compose, GitHub Actions, Render blueprint |

## Features

- **Multi-role auth**: Admin, Organizer, Participant (JWT, BCrypt, role + ownership checks), **forgot password** with a 6-digit code e-mailed to the user
- **Event lifecycle**: Draft → Published → Completed / Cancelled, with live phase (Upcoming, Live, Ended)
- **Session scheduling**: blocks speaker double-booking, room clashes and sessions outside the event
- **Speaker management**: a shared directory
- **Smart registration**: live capacity, **FIFO waitlist with automatic promotion**, duplicate and schedule-clash guards, race-safe row locking
- **QR tickets**: unguessable codes, printable ticket, registration timeline
- **QR attendance scanner**: camera or typed code, session-wise, duplicate-scan detection, clear refusal reasons
- **Attendance-verified feedback**: only people who attended can rate
- **AI**: one-click event descriptions and feedback summaries (sentiment, strengths, improvements, action items)
- **Analytics**: fill rate, attendance, no-shows, session popularity, rating distribution, 14-day trend, category mix
- **CSV export**, dark mode, glassmorphism UI, animated stats, responsive layout

See [docs/BUSINESS-PROCESS.md](docs/BUSINESS-PROCESS.md) for the process model and the 20 business rules.

## Quick start

### Option 1: No database install (fastest, good for the demo)
Requires **Java 21+**, **Maven 3.9+**, **Node 20.19+/22**.
```bash
# terminal 1: API on http://localhost:8080 (embedded H2, demo data loaded)
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
```bash
# terminal 2: UI on http://localhost:5173
cd frontend
npm install
npm run dev
```

### Option 2: With your local MySQL
```bash
cd backend
# PowerShell: $env:DB_USERNAME="root"; $env:DB_PASSWORD="yourpassword"; mvn spring-boot:run
DB_USERNAME=root DB_PASSWORD=yourpassword mvn spring-boot:run
```
The `eventsphere` database and tables are created automatically (Flyway). Then start the frontend as above.

### Option 3: Docker (MySQL + API + UI)
```bash
cp .env.example .env
docker compose up --build
```
UI http://localhost:3000 · API http://localhost:8080

> **Port 8080 busy?** Run the API with `PORT=8081` (PowerShell: `$env:PORT=8081`) and create
> `frontend/.env.local` containing `VITE_PROXY_TARGET=http://localhost:8081`.

### Demo accounts (one-click buttons on the login page, local development only)
> On a public deployment set `DISABLE_DEMO_ACCOUNTS=true` (these passwords are public) and `ADMIN_EMAILS=<your email>`;
> the one-click buttons are hidden automatically in production builds.

| Role | Email | Password |
|---|---|---|
| Admin | admin@eventsphere.com | Admin@123 |
| Organizer | organizer@eventsphere.com | Organizer@123 |
| Participant | participant@eventsphere.com | Participant@123 |

Demo data is generated relative to *today*: a **live** workshop (full, with waitlist), an upcoming hackathon
that is **full with a waitlist**, an open corporate summit, a draft ready to publish, a cancelled webinar and a
**completed** conference with attendance and feedback.

### Password reset e-mails (optional)
Easiest on Windows: create a Gmail *app password* (https://myaccount.google.com/apppasswords, needs 2-Step
Verification), then run `powershell -ExecutionPolicy Bypass -File .
un-backend.ps1`. It asks for your MySQL
password, Gmail address and app password (typed hidden, never saved) and starts the backend with e-mail on.

Manual setup:
Set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` (e.g. Gmail with an app password). Without them,
demo mode shows the reset code on screen after *Forgot password?*.

### Enable Gemini (optional)
Get a key at https://aistudio.google.com/apikey and set `GEMINI_API_KEY` before starting the backend.
Without it, the AI buttons still work using the built-in generator (labelled in the UI).

## Useful URLs
| URL | What |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI (click *Authorize*, paste the token from `/api/auth/login`) |
| http://localhost:8080/actuator/health | Health check |
| http://localhost:8080/h2-console | H2 console (h2 profile; JDBC URL `jdbc:h2:file:./data/eventsphere`) |

## Tests
```bash
cd backend
mvn test
```
47 automated tests cover every business rule (registration/waitlist, scheduling clashes, lifecycle, check-in,
feedback eligibility, password reset, security, AI fallback). CI runs them on every push.

## Project structure
```
Event-Management-Website/
├── backend/                         Spring Boot REST API
│   ├── src/main/java/com/eventsphere/
│   │   ├── config/                  Security (JWT), OpenAPI, Clock, demo data seeder
│   │   ├── controller/              REST endpoints (thin: HTTP ↔ service)
│   │   ├── dto/                     Request/response records + validation
│   │   ├── entity/                  JPA entities and enums (state machines)
│   │   ├── exception/               Error types + global JSON error handler
│   │   ├── repository/              Spring Data repositories and queries
│   │   ├── security/                JWT issuing, current-user resolution
│   │   └── service/                 ★ business rules (EventService, RegistrationService,
│   │       └── ai/                    WaitlistService, AttendanceService, …) + Gemini client/fallback
│   ├── src/main/resources/db/migration/   Flyway SQL migrations
│   ├── src/test/                    Integration + unit tests
│   └── Dockerfile
├── frontend/                        React SPA
│   ├── src/pages/                   Landing, Auth, Dashboard, Events, EventDetail, EventForm,
│   │                                MyTickets, CheckIn, Speakers, Users
│   ├── src/components/              UI kit, app shell, charts, QR ticket, timeline
│   │   └── event/                   Agenda, Registrations, Attendance, Analytics, Participant panel
│   ├── src/context/                 Auth, theme, toasts
│   ├── src/lib/                     API client, formatting, constants
│   ├── Dockerfile, nginx.conf
├── database/schema.sql              Reference MySQL schema
├── docs/                            Business process, SRS (IEEE), API, deployment, AI usage log
├── .github/workflows/ci.yml         Test → build → docker → deploy
├── docker-compose.yml
└── render.yaml                      Render blueprint
```

## Documentation
| Doc | Contents |
|---|---|
| [BUSINESS-PROCESS.md](docs/BUSINESS-PROCESS.md) | Problem, actors, process flow, state machines, rules R1-R20, metrics |
| [SRS-IEEE.md](docs/SRS-IEEE.md) | IEEE-830 style requirements with test traceability |
| [API.md](docs/API.md) | All REST endpoints |
| [DEPLOYMENT.md](docs/DEPLOYMENT.md) | Docker, Render, environment variables, production troubleshooting |
| [DATABASE-WALKTHROUGH.md](docs/DATABASE-WALKTHROUGH.md) | MySQL queries to show in the interview, and what each one proves |
| [AI-USAGE.md](docs/AI-USAGE.md) | AI tools, prompts, design decisions, learnings, demo script |
