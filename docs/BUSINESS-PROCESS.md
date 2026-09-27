# EventSphere: Business Process & Rules

> Assessment topic #10, **Event Management Platform**: *manage event registration, participants, sessions,
> attendance, speakers and post-event feedback.*

## 1. The business problem

Colleges, companies and conference organisers typically run events with spreadsheets, e-mail threads,
paper sign-in sheets and Google Forms. That causes:

| Pain | Consequence |
|---|---|
| No live seat count | Overbooking, or turning people away while seats are actually free |
| No waitlist | A cancelled seat stays empty; the next interested person never hears about it |
| Manual agenda | The same speaker or room gets double-booked |
| Paper attendance | Nobody knows who actually came, or which sessions were popular |
| Open feedback forms | People who never attended can rate the event, so the ratings can't be trusted |
| Scattered data | Organisers cannot learn from one event to plan the next |

**EventSphere runs the full lifecycle in one system:**
**Plan → Publish → Register → Check-in → Close → Feedback → Analyse**

## 2. Actors

| Role | How they get it | Responsibilities |
|---|---|---|
| **Admin** | Seeded | Governs the platform, promotes users to organizer, deactivates accounts, can manage every event |
| **Organizer** | Promoted by an admin | Creates and runs **their own** events: agenda, publishing, check-in, closing, reports |
| **Participant** | Self sign-up | Registers for events, holds QR tickets, tracks waitlist position, gives feedback |
| **Speaker** | Master data (no login) | Assigned to sessions; shared directory across organizers |

**Forgot password:** the user enters their e-mail and receives a 6-digit code (valid 10 min, single use, locked
after 5 wrong attempts). The screen shows the same message whether or not the e-mail is registered.

**Becoming an organizer:** a participant opens *Become an organizer* and submits a short reason. Admins get an
e-mail; in *Users & roles* they approve (the role becomes ORGANIZER immediately) or reject with a note. Only one
request can be pending at a time; after a rejection the participant may apply again.

*Why organizers are approved, not self-registered:* anyone publishing events on the platform should be vetted.

## 3. End-to-end process

```
 ORGANIZER                                          PARTICIPANT
 1. Add speakers (shared directory)
 2. Create event  ── status DRAFT (invisible)
 3. Build agenda  ── sessions checked for speaker/room clashes
    (optional) AI writes the description
 4. Publish ───────────────────────────────────────► 5. Discover & register
                                                        ├─ seat free   → CONFIRMED + QR ticket
                                                        └─ event full  → WAITLISTED (#position)
                                                     6. Cancel (before start)
                                                        └─ seat auto-given to waitlist #1
 7. Event day: scan QR tickets per session ◄─────── shows QR at the door
 8. Mark event COMPLETED (locks attendance) ───────► 9. Rate the event (attendees only)
10. Analytics: fill rate, attendance, no-shows,
    session popularity, ratings, AI feedback summary
```

### Event state machine

```
            publish                   complete
  DRAFT ──────────────► PUBLISHED ──────────────► COMPLETED
    │                       │    (phase: UPCOMING → LIVE → ENDED, derived from time)
    │ cancel                │ cancel
    ▼                       ▼
  CANCELLED             CANCELLED
```

*Status* is what the organizer decided; *phase* is what is happening right now. This keeps the stored state
simple while the UI can still say "Live now".

### Registration state machine

```
 register ─┬─ confirmed < capacity ──► CONFIRMED ──cancel──► CANCELLED ──re-register──► (back of the queue)
           │                               ▲
           └─ event full ──► WAITLISTED ───┘  promoted FIFO when a seat frees up
                                 └──cancel──► CANCELLED
```

## 4. Business rules (enforced in the service layer, covered by tests)

**Events**
- **R1** End after start; registration deadline on or before the start; a new event must start in the future.
- **R2** Publish only a DRAFT that has **≥ 1 session** and a deadline still in the future.
- **R3** Capacity can't be reduced below the number of confirmed participants.
- **R4** Increasing capacity promotes waitlisted people automatically (FIFO).
- **R5** Event dates can't change so that an existing session falls outside them.
- **R6** Only drafts can be deleted; completed/cancelled events are read-only (history is kept).
- **R7** An event can be completed only after it has started.
- **Ownership:** organizers manage only their own events; admins manage all. Drafts are invisible to participants.

**Sessions & speakers**
- **R8** A session lies within the event window and ends after it starts.
- **R9** A speaker can't be in two overlapping sessions, across all active events.
- **R10** Two sessions of the same event can't use the same room at the same time.
- **R11** A speaker assigned to sessions can't be deleted.

**Registration (smart registration)**
- **R12** Only for PUBLISHED events, before the deadline; only participants register.
- **R13** One registration per participant per event; a cancelled one can be re-activated (goes to the back of the queue).
- **R14** Confirmed if seats remain, otherwise waitlisted.
- **R15** Waitlist is strictly FIFO; the participant sees their position.
- **R16** Cancellation only before the event starts; a freed seat is given to waitlist #1 immediately.
- **Clash guard:** a participant can't hold confirmed seats at two events that overlap in time.
- **Concurrency:** seat allocation runs under a database row lock on the event, so two people can never take the last seat.

**Attendance (QR check-in)**
- **R17** Only CONFIRMED tickets check in; waitlisted/cancelled tickets and tickets for another event are refused with a reason.
- **R18** Check-in opens 30 min before a session and closes when the event ends; attendance locks when the event is completed.
- Scanning the same ticket twice is **not an error**: it answers "already checked in at 10:02", which is what door staff need.
- Ticket codes (`ES-7KQ4-M9XD`) are random, unguessable and exclude look-alike characters so they can be typed if a camera fails.

**Feedback**
- **R19** Opens only when the event is COMPLETED.
- **R20** Only participants who attended ≥ 1 session, once per event. Ratings therefore reflect real attendees.

## 5. What the organizer learns (analytics)

| Metric | Formula | Decision it supports |
|---|---|---|
| Fill rate | confirmed ÷ capacity | Venue sizing, marketing effort |
| Waitlist promotions | count | Demand beyond capacity → bigger venue next time |
| Attendance rate | attendees ÷ confirmed | Over-booking policy |
| No-shows | confirmed − attendees | Reminder strategy |
| Session popularity | check-ins per session | Agenda design, speaker selection |
| Ratings & recommend % | averages of verified feedback | Quality of content vs organisation |
| AI summary | Gemini (or rule-based fallback) | Concrete action items for the next event |

## 6. Assumptions
- One organisation runs the platform; events are free (no payment step).
- Speakers don't log in.
- Times are local wall-clock times (server time zone configurable, default Asia/Kolkata).
- Completing an event is a deliberate organizer action (they may close it on the last day).
- AI is an assistant, not a dependency: every AI feature has a deterministic fallback.

## 7. Data model

```
 users (ADMIN | ORGANIZER | PARTICIPANT)
   │1                 organizes
   ├──────────────────────────────────► events ◄──────────────┐
   │1                                    │1                     │
   │ *                                   │ *                    │
 registrations  (event, participant) UNIQUE, status, ticket_code UNIQUE, registered_at (queue order)
   │1                                    │
   │ *                                   ▼ *
 attendance (registration, session) UNIQUE ──► event_sessions ──* speakers
 feedback   (event, participant) UNIQUE, rating/content/organisation 1-5, would_recommend
```
