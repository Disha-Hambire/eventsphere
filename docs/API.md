# EventSphere REST API

Base URL: `http://localhost:8080/api` · Interactive docs: **`/swagger-ui.html`** (click *Authorize*, paste the token).

All endpoints except those marked **public** need `Authorization: Bearer <JWT>`.
Errors always have the same shape, so the UI can show `message` directly:

```json
{ "timestamp": "2026-10-01T10:00:00", "status": 422, "error": "Unprocessable Entity",
  "message": "Cannot publish: add at least one session to the agenda first",
  "path": "/api/events/5/publish", "fieldErrors": null }
```

| Status | Meaning |
|---|---|
| 400 | Invalid input (`fieldErrors` lists each field) |
| 401 | Not logged in / bad credentials / deactivated |
| 403 | Logged in but not allowed (wrong role or not your event) |
| 404 | Not found (drafts are 404 for participants) |
| 409 | Duplicate / conflict (already registered, email taken…) |
| 422 | Business rule violated: the message says which rule |

## Authentication
| Method | Path | Who | Description |
|---|---|---|---|
| POST | `/auth/register` | public | Sign up (always PARTICIPANT) → `{token, user}` |
| POST | `/auth/login` | public | `{email, password}` → `{token, user}` |
| GET | `/auth/me` | any | Current user |
| POST | `/auth/forgot-password` | public | `{email}` → e-mails a 6-digit code; same message whether or not the account exists; `demoCode` only in demo mode (no SMTP) |
| POST | `/auth/reset-password` | public | `{email, code, newPassword}`: code is single-use, expires in 10 min, locked after 5 wrong attempts |

## Public
| GET | `/public/stats` | public | Totals for the landing page |
|---|---|---|---|
| GET | `/public/events` | public | Upcoming published events |

## Events
| Method | Path | Who | Description |
|---|---|---|---|
| GET | `/events?status=&category=&q=` | any | Participant: published + completed · Organizer: own · Admin: all |
| GET | `/events/{id}` | any | Detail + agenda + caller's registration (`myRegistration`, `canRegister`, `canGiveFeedback`) |
| POST | `/events` | organizer, admin | Create (DRAFT) |
| PUT | `/events/{id}` | owner, admin | Update (capacity increase promotes waitlist) |
| DELETE | `/events/{id}` | owner, admin | Delete a draft |
| POST | `/events/{id}/publish` | owner, admin | DRAFT → PUBLISHED |
| POST | `/events/{id}/complete` | owner, admin | PUBLISHED → COMPLETED (opens feedback) |
| POST | `/events/{id}/cancel` | owner, admin | → CANCELLED |
| GET | `/events/{id}/sessions` | any | Agenda |
| POST | `/events/{id}/sessions` | owner, admin | Add session (speaker/room clash checks) |

Event request body:
```json
{ "title": "Campus Hackathon 2026", "description": "…", "category": "COLLEGE", "venue": "Main Auditorium",
  "startDateTime": "2026-10-12T09:00", "endDateTime": "2026-10-13T18:00",
  "registrationDeadline": "2026-10-10T23:59", "capacity": 120 }
```
Categories: `CONFERENCE, CORPORATE, COLLEGE, WORKSHOP, MEETUP, WEBINAR, CULTURAL, SPORTS`.

## Sessions & attendance
| Method | Path | Who | Description |
|---|---|---|---|
| PUT | `/sessions/{id}` | owner, admin | Update session |
| DELETE | `/sessions/{id}` | owner, admin | Delete (only if no attendance recorded) |
| GET | `/sessions/{id}/attendance` | owner, admin | Attendance sheet (all confirmed participants) |
| PUT | `/sessions/{id}/attendance/{registrationId}` | owner, admin | Mark present manually |
| DELETE | `/sessions/{id}/attendance/{registrationId}` | owner, admin | Undo check-in |

## Registrations & QR check-in
| Method | Path | Who | Description |
|---|---|---|---|
| POST | `/events/{id}/registrations` | participant | Register → `{registration, message}` (CONFIRMED or WAITLISTED #n) |
| GET | `/events/{id}/registrations` | owner, admin | All registrations with timeline |
| GET | `/events/{id}/registrations/export` | owner, admin | CSV download |
| GET | `/registrations/my` | participant | My tickets |
| GET | `/registrations/{id}` | owner of registration, event owner, admin | One registration + timeline |
| POST | `/registrations/{id}/cancel` | owner of registration, event owner, admin | Cancel → `{registration, message, promoted}` |
| POST | `/check-in` | owner, admin | `{ticketCode, sessionId}` → `CHECKED_IN` or `ALREADY_CHECKED_IN` |

## Feedback & analytics
| Method | Path | Who | Description |
|---|---|---|---|
| POST | `/events/{id}/feedback` | participant (attended) | `{rating, contentRating, organizationRating, wouldRecommend, comments}` |
| GET | `/events/{id}/feedback` | owner, admin | All feedback |
| GET | `/events/{id}/report` | owner, admin | Fill rate, attendance, no-shows, sessions, ratings, cached AI summary |
| POST | `/events/{id}/ai-summary` | owner, admin | Generate AI feedback summary |
| GET | `/dashboard` | organizer, admin | Portfolio analytics |

## AI
| GET | `/ai/status` | organizer, admin | Is Gemini configured? |
|---|---|---|---|
| POST | `/ai/event-description` | organizer, admin | `{title, category, venue, audience, highlights, tone}` → `{text, source: GEMINI\|FALLBACK}` |

## Organizer requests
| Method | Path | Who | Description |
|---|---|---|---|
| POST | `/organizer-requests` | participant | `{organization, reason}`: apply to become an organizer (one pending at a time) |
| GET | `/organizer-requests/my` | any | My latest request (204 if none) |
| GET | `/admin/organizer-requests?status=PENDING\|APPROVED\|REJECTED\|ALL` | admin | List requests |
| GET | `/admin/organizer-requests/count` | admin | `{pending}` |
| POST | `/admin/organizer-requests/{id}/approve` | admin | `{note?}`: applicant becomes ORGANIZER immediately |
| POST | `/admin/organizer-requests/{id}/reject` | admin | `{note?}`: applicant is told and may apply again |

## Speakers & users
| Method | Path | Who | Description |
|---|---|---|---|
| GET | `/speakers` | any | Directory |
| POST / PUT / DELETE | `/speakers[/{id}]` | organizer, admin | Manage (delete blocked if assigned) |
| GET | `/admin/users` | admin | All users |
| PATCH | `/admin/users/{id}/role` | admin | `{role}` |
| PATCH | `/admin/users/{id}/status` | admin | `{active}` |

## Quick try with curl
```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"organizer@eventsphere.com","password":"Organizer@123"}' | jq -r .token)
curl -s localhost:8080/api/dashboard -H "Authorization: Bearer $TOKEN" | jq
```
