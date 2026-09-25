# How AI Was Used to Build EventSphere

The assessment asks candidates to explain *which AI tools were used, the prompts, the interaction and the
learnings*. This is a working log to prepare that part of the demo. **Fill in your own words where marked ✍️**:
the interviewers want to hear your reasoning, not a script.

## Tools
| Tool | Used for |
|---|---|
| Claude (Claude Code) | Requirement analysis, process design, backend + frontend code, tests, Docker/CI, documentation |
| Google Gemini (inside the product) | AI event descriptions and feedback summaries |
| ✍️ (others you used: ChatGPT, Copilot…) | |

## Lifecycle: how the conversation went

1. **Understand**: shared the assessment PDF and asked what it expects. Learned the primary criterion is
   *business process understanding*, not technical complexity.
2. **Analyse**: asked for the problem statement behind the one-line topic ("manage registration, participants,
   sessions, attendance, speakers, feedback"), which became the pain-point table in `BUSINESS-PROCESS.md`.
3. **Design**: agreed the lifecycle *Plan → Publish → Register → Check-in → Close → Feedback → Analyse*, two state
   machines (event, registration) and 20 numbered business rules **before** writing code.
4. **Decide the stack**: compared Thymeleaf vs React and H2 vs MySQL; chose React + Spring Boot + MySQL.
5. **Build**: backend first (entities → Flyway schema → services with rules → controllers), then tests, then UI.
6. **Review & fix**: running the app exposed real bugs that were then fixed (see *Learnings*).

### Key prompts (paraphrased)
- "What is the problem statement for this topic?"
- "What features would impress the interviewer?" → prioritised by *process value*, not flashiness.
- "Build EventSphere … React 19 + Vite + Tailwind, Spring Boot 3 (Java 21), MySQL, JWT, Docker, Render, Gemini …
  smart registration, waitlist, QR tickets & scanner, sessions, speakers, AI feedback summarisation, analytics."
- ✍️ add any follow-up prompts you use while customising

## Design decisions worth explaining
| Decision | Why |
|---|---|
| Business rules only in the **service layer** | Enforced for every client (UI, Swagger, curl); easy to test |
| **Status vs phase** | Store only decisions (DRAFT/PUBLISHED/…); derive UPCOMING/LIVE/ENDED from time, so nothing is stale |
| **Pessimistic lock** on the event row during registration | Two people can never take the last seat at the same moment |
| **FIFO waitlist by timestamp** + auto-promotion | Fair and fully automatic; no manual chasing |
| Re-registering reuses the row | One row per (event, participant) keeps history and uniqueness simple |
| Check-in by **random ticket code**, not user id | Unguessable; can be typed if the camera fails; duplicate scans are informative, not errors |
| Feedback only from **attendees** | Makes ratings trustworthy (the core pain point) |
| Organizers **promoted by admin** | Nobody can publish events without being vetted |
| **Flyway** migrations | Versioned, reviewable schema; Hibernate never alters tables |
| AI with **fallback** | The product (and the live demo) never depends on an external API being up |
| `Clock` injected everywhere | Time-based rules (deadlines, check-in window) are testable |

## Learnings (real issues found while building)
1. **Timestamp precision bug.** Windows clocks have 100 ns precision but MySQL `DATETIME(6)` stores microseconds.
   A re-registering participant counted *themselves* ahead in the queue ("#3" instead of "#2"). Fix: truncate
   to microseconds in the entity + a regression test. *Lesson: test with realistic data, not round numbers.*
2. **Validation vs authorisation order.** A participant posting an empty event got 400 instead of 403, because
   request validation runs before method security. The test was made to send a valid body so it checks authorisation.
3. **Glass UI performance.** Many `backdrop-blur` cards over an animated background forced constant re-blurring.
   The animation was limited to the landing page and blur reduced.
4. **Animated counters showing 0.** `requestAnimationFrame`/`IntersectionObserver` pause in hidden tabs; a
   fallback timer now always lands the real number.
5. **CSV column shift.** Dates like "14 Sep 2026, 10:00 AM" contain a comma; unquoted, Excel shifted every
   column after it. Found by testing the export on real MySQL data; fixed with quoting + a column-count test.
6. **Forgot password (link → e-mailed 6-digit code).** Wrong guesses must be counted even though the request fails,
   so the transaction is configured not to roll back on rule errors (`noRollbackFor`). A bulk "invalidate old codes"
   query also left stale objects in the Hibernate session; replaced with plain entity updates. Codes are BCrypt-hashed,
   expire in 10 min, and lock after 5 wrong attempts; the response never reveals whether an e-mail is registered.
7. ✍️ your own learnings about prompting (e.g. being specific about rules gave better code than asking for "an app")

## Suggested 10-minute demo script
1. **Problem** (1 min): pain-point table → lifecycle diagram.
2. **Organizer** (3 min): dashboard → create event → *Write with AI* → add sessions (show a **speaker clash**
   error) → publish (try before adding sessions to show the rule).
3. **Participant** (2 min): Campus Hackathon is full → cancel as Ananya → toast says the seat went to Riya →
   re-join → "#2 on the waitlist". Show QR ticket and timeline.
4. **Event day** (2 min): QR check-in on the *live* workshop (scan from phone or type the code), scan again →
   "already checked in", try a fake code → refused with reason.
5. **After the event** (2 min): TechNova Summit analytics → attendance rate, no-shows, session popularity →
   *Generate AI summary* → action items. Mention tests (`mvn test`) and CI.
