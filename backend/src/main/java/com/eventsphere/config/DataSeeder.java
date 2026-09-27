package com.eventsphere.config;

import com.eventsphere.entity.*;
import com.eventsphere.repository.*;
import com.eventsphere.service.TicketCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a realistic demo data set on first start (empty database only). All dates are relative to "now",
 * so whenever the app is started there is: a LIVE workshop, upcoming events (one full with a waitlist),
 * a draft, a cancelled event, and a completed conference with attendance and feedback.
 *
 * Demo logins:  admin@eventsphere.com / Admin@123
 *               organizer@eventsphere.com / Organizer@123
 *               participant@eventsphere.com / Participant@123
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final boolean enabled;
    private final UserRepository users;
    private final SpeakerRepository speakers;
    private final EventRepository events;
    private final EventSessionRepository sessions;
    private final RegistrationRepository registrations;
    private final AttendanceRepository attendance;
    private final FeedbackRepository feedback;
    private final PasswordEncoder passwordEncoder;
    private final TicketCodeGenerator ticketCodes;
    private final TransactionTemplate tx;
    private final Clock clock;

    public DataSeeder(@Value("${app.seed-demo-data:true}") boolean enabled, UserRepository users,
                      SpeakerRepository speakers, EventRepository events, EventSessionRepository sessions,
                      RegistrationRepository registrations, AttendanceRepository attendance,
                      FeedbackRepository feedback, PasswordEncoder passwordEncoder,
                      TicketCodeGenerator ticketCodes, TransactionTemplate tx, Clock clock) {
        this.enabled = enabled;
        this.users = users;
        this.speakers = speakers;
        this.events = events;
        this.sessions = sessions;
        this.registrations = registrations;
        this.attendance = attendance;
        this.feedback = feedback;
        this.passwordEncoder = passwordEncoder;
        this.ticketCodes = ticketCodes;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (!enabled || users.count() > 0) {
            return;
        }
        tx.executeWithoutResult(status -> seed());
        log.info("Demo data loaded. Log in as admin@eventsphere.com / Admin@123, organizer@eventsphere.com / "
                + "Organizer@123 or participant@eventsphere.com / Participant@123");
    }

    private void seed() {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.HOURS);
        LocalDateTime today = now.toLocalDate().atStartOfDay();

        // ---------------------------------------------------------------- users
        user("Aditi Rao", "admin@eventsphere.com", "Admin@123", Role.ADMIN, "EventSphere");
        User priya = user("Priya Sharma", "organizer@eventsphere.com", "Organizer@123", Role.ORGANIZER, "TechCircle Community");
        User rahul = user("Rahul Verma", "organizer2@eventsphere.com", "Organizer@123", Role.ORGANIZER, "Campus Events Cell");
        User demo = user("Ananya Iyer", "participant@eventsphere.com", "Participant@123", Role.PARTICIPANT, "Pune Institute of Technology");

        String[][] people = {
                {"Riya Kapoor", "riya@example.com"}, {"Arjun Singh", "arjun@example.com"},
                {"Sneha Patil", "sneha@example.com"}, {"Vikram Joshi", "vikram@example.com"},
                {"Neha Gupta", "neha@example.com"}, {"Karan Malhotra", "karan@example.com"},
                {"Pooja Desai", "pooja@example.com"}, {"Aditya Kulkarni", "aditya@example.com"},
                {"Meera Nair", "meera@example.com"}, {"Rohan Das", "rohan@example.com"},
                {"Isha Bhatt", "isha@example.com"}, {"Siddharth Rao", "siddharth@example.com"}};
        List<User> p = new ArrayList<>();
        for (String[] person : people) {
            p.add(user(person[0], person[1], "Participant@123", Role.PARTICIPANT, null));
        }

        // ---------------------------------------------------------------- speakers
        Speaker kavita = speakers.save(new Speaker("Dr. Kavita Rao", "kavita.rao@neuralworks.example", "NeuralWorks Labs",
                "Head of AI Research", "Leads applied generative-AI research and has mentored 40+ startups on responsible AI.",
                "Generative AI, LLMs, Responsible AI"));
        Speaker sameer = speakers.save(new Speaker("Sameer Khan", "sameer@skyscale.example", "SkyScale Cloud",
                "Principal Cloud Architect", "Designs multi-region cloud platforms serving millions of users.",
                "Cloud architecture, Kubernetes, DevOps"));
        Speaker lisa = speakers.save(new Speaker("Lisa Fernandes", "lisa@pixelcraft.example", "PixelCraft Studio",
                "Director of Product Design", "Builds design systems for fintech and SaaS products.",
                "Product design, Design systems, UX research"));
        Speaker vivek = speakers.save(new Speaker("Vivek Menon", "vivek@launchpad.example", "LaunchPad Ventures",
                "Founder & Partner", "Serial entrepreneur and early-stage investor.", "Startups, Fundraising, Product strategy"));
        Speaker nehaK = speakers.save(new Speaker("Dr. Neha Kulkarni", "neha.kulkarni@college.example", "Pune Institute of Technology",
                "Professor of Computer Science", "Researcher in distributed systems and hackathon mentor.",
                "Distributed systems, Algorithms"));
        Speaker farhan = speakers.save(new Speaker("Farhan Ali", "farhan@codenest.example", "CodeNest",
                "Engineering Manager", "Scales engineering teams and ships developer platforms.",
                "Engineering leadership, Platform engineering"));

        // ---------------------------------------------------------------- E1: completed conference
        LocalDateTime d1 = today.minusDays(20);
        Event summit = event("TechNova Summit 2026", EventCategory.CONFERENCE, "Grand Hyatt Convention Hall, Pune",
                d1.plusHours(9), d1.plusHours(18), d1.minusDays(1), 150, priya, EventStatus.COMPLETED,
                "A full-day technology conference on AI, cloud and product design with industry leaders, "
                        + "live demos and a founders' panel.");
        EventSession keynote = session(summit, "Keynote: The Next Decade of Generative AI", kavita, "Main Hall", d1.plusHours(9).plusMinutes(30), 60);
        EventSession cloudTalk = session(summit, "Cloud-Native at Scale", sameer, "Hall B", d1.plusHours(11), 60);
        EventSession designTalk = session(summit, "Design Systems that Scale", lisa, "Hall B", d1.plusHours(14), 60);
        EventSession panel = session(summit, "Founders' Panel: Building in 2026", vivek, "Main Hall", d1.plusHours(15).plusMinutes(30), 90);

        List<Registration> summitRegs = new ArrayList<>();
        summitRegs.add(reg(summit, demo, RegistrationStatus.CONFIRMED, d1.minusDays(9)));
        for (int i = 0; i < 10; i++) {
            summitRegs.add(reg(summit, p.get(i), RegistrationStatus.CONFIRMED, d1.minusDays(10 - i % 8).plusHours(i)));
        }
        Registration promoted = reg(summit, p.get(10), RegistrationStatus.WAITLISTED, d1.minusDays(4));
        promoted.promoteFromWaitlist(d1.minusDays(3));
        summitRegs.add(promoted);
        reg(summit, p.get(11), RegistrationStatus.CONFIRMED, d1.minusDays(8)).cancel(d1.minusDays(3));

        // attendance: 10 of 12 confirmed showed up (2 no-shows)
        EventSession[] summitSessions = {keynote, cloudTalk, designTalk, panel};
        for (int i = 0; i < summitRegs.size(); i++) {
            Registration r = summitRegs.get(i);
            if (i == 7 || i == 9) {
                continue; // no-shows
            }
            for (int s = 0; s < summitSessions.length; s++) {
                if (i == 0 && s > 0) {
                    break; // demo participant attended the keynote only
                }
                if ((i + s) % 3 != 2 || s == 0) {
                    attend(r, summitSessions[s], summitSessions[s].getStartTime().minusMinutes(10 - i % 10));
                }
            }
        }
        String[][] comments = {
                {"5", "5", "4", "true", "The keynote on generative AI was outstanding and very insightful. Great networking with peers during breaks."},
                {"4", "5", "3", "true", "Excellent speakers and content, but the hall was crowded and the Wi-Fi kept dropping."},
                {"5", "4", "5", "true", "Very well organised. The founders' panel was the highlight - loved the practical advice."},
                {"3", "4", "2", "false", "Good talks but the schedule overran by 40 minutes and lunch was late. Audio in Hall B was poor."},
                {"4", "4", "4", "true", "Loved the design systems session. Would like more hands-on demos next time."},
                {"5", "5", "4", "true", "Inspiring content and great speakers. Coffee breaks were too short to network."},
                {"4", "3", "4", "true", "Cloud-native talk was a bit too advanced, but overall a great day and good community."}};
        int c = 0;
        for (int i = 1; i < summitRegs.size() && c < comments.length; i++) {
            if (i == 7 || i == 9) {
                continue;
            }
            String[] f = comments[c++];
            feedback.save(new Feedback(summit, summitRegs.get(i).getParticipant(), Integer.parseInt(f[0]),
                    Integer.parseInt(f[1]), Integer.parseInt(f[2]), Boolean.parseBoolean(f[3]), f[4],
                    d1.plusDays(1).plusHours(i)));
        }

        // ---------------------------------------------------------------- E2: LIVE workshop (full + waitlist)
        LocalDateTime s2 = now.minusHours(2);
        Event workshop = event("Cloud & GenAI Hands-on Workshop", EventCategory.WORKSHOP, "TechCircle Innovation Lab, Baner",
                s2, s2.plusHours(8), s2.minusDays(1), 8, priya, EventStatus.PUBLISHED,
                "A practical, laptop-required workshop: build and deploy a retrieval-augmented GenAI app on the cloud.");
        EventSession w1 = session(workshop, "Setting up your GenAI toolkit", sameer, "Lab 1", s2, 60);
        session(workshop, "Building RAG apps with LLMs", kavita, "Lab 1", s2.plusMinutes(90), 90);
        session(workshop, "Deploying AI apps to the cloud", farhan, "Lab 1", s2.plusHours(4), 90);
        session(workshop, "Showcase & wrap-up", null, "Lab 1", s2.plusMinutes(390), 60);
        List<Registration> workshopRegs = new ArrayList<>();
        workshopRegs.add(reg(workshop, demo, RegistrationStatus.CONFIRMED, today.minusDays(6).plusHours(11)));
        for (int i = 0; i < 7; i++) {
            workshopRegs.add(reg(workshop, p.get(i + 2), RegistrationStatus.CONFIRMED, today.minusDays(12 - i).plusHours(10 + i)));
        }
        reg(workshop, p.get(9), RegistrationStatus.WAITLISTED, today.minusDays(3).plusHours(15));
        reg(workshop, p.get(10), RegistrationStatus.WAITLISTED, today.minusDays(2).plusHours(9));
        for (int i = 1; i <= 6; i++) {
            attend(workshopRegs.get(i), w1, s2.minusMinutes(15 - i));
        }

        // ---------------------------------------------------------------- E3: upcoming hackathon, FULL with waitlist
        LocalDateTime d3 = today.plusDays(12);
        Event hackathon = event("Campus Hackathon 2026", EventCategory.COLLEGE, "Pune Institute of Technology, Main Auditorium",
                d3.plusHours(9), d3.plusDays(1).plusHours(18), d3.minusDays(2).plusHours(23).plusMinutes(59), 5, rahul,
                EventStatus.PUBLISHED,
                "A 36-hour inter-college hackathon. Build solutions for real campus problems with mentors from industry.");
        session(hackathon, "Opening & problem statements", nehaK, "Main Auditorium", d3.plusHours(9), 60);
        session(hackathon, "Hacking round 1", null, "Labs 1-4", d3.plusHours(10).plusMinutes(30), 450);
        session(hackathon, "Mentor connect", farhan, "Seminar Hall", d3.plusHours(15), 90);
        session(hackathon, "Final demos & judging", vivek, "Main Auditorium", d3.plusDays(1).plusHours(14), 180);
        reg(hackathon, demo, RegistrationStatus.CONFIRMED, today.minusDays(5).plusHours(10));
        for (int i = 0; i < 4; i++) {
            reg(hackathon, p.get(i + 4), RegistrationStatus.CONFIRMED, today.minusDays(9 - i).plusHours(12));
        }
        reg(hackathon, p.get(0), RegistrationStatus.WAITLISTED, today.minusDays(2).plusHours(18));
        reg(hackathon, p.get(1), RegistrationStatus.WAITLISTED, today.minusDays(1).plusHours(10));

        // ---------------------------------------------------------------- E4: upcoming corporate summit (open)
        LocalDateTime d4 = today.plusDays(5);
        Event leadership = event("Leadership Summit: Future of Work", EventCategory.CORPORATE, "The Westin, Koregaon Park",
                d4.plusHours(10), d4.plusHours(17), d4.minusDays(1).plusHours(18), 60, priya, EventStatus.PUBLISHED,
                "An executive summit on hybrid teams, AI-assisted productivity and building resilient organisations.");
        session(leadership, "Leading hybrid teams", farhan, "Ballroom", d4.plusHours(10).plusMinutes(30), 60);
        session(leadership, "AI copilots in the workplace", kavita, "Ballroom", d4.plusHours(12), 60);
        session(leadership, "Designing for employee experience", lisa, "Ballroom", d4.plusHours(14).plusMinutes(30), 60);
        for (int i = 0; i < 6; i++) {
            reg(leadership, p.get(i + 3), RegistrationStatus.CONFIRMED, today.minusDays(13 - 2 * i).plusHours(14));
        }

        // ---------------------------------------------------------------- E5: draft (ready to publish in the demo)
        LocalDateTime d5 = today.plusDays(20);
        Event meetup = event("Design Systems Meetup", EventCategory.MEETUP, "PixelCraft Studio, Kalyani Nagar",
                d5.plusHours(18), d5.plusHours(21), d5.minusDays(1), 40, priya, EventStatus.DRAFT,
                "An evening meetup for designers and front-end engineers on building and scaling design systems.");
        session(meetup, "Tokens, themes and dark mode", lisa, "Studio Floor", d5.plusHours(18).plusMinutes(30), 60);

        // ---------------------------------------------------------------- E6: cultural fest
        LocalDateTime d6 = today.plusDays(25);
        Event fest = event("Rhythm 2026 - Inter-College Cultural Fest", EventCategory.CULTURAL, "College Open Air Theatre",
                d6.plusHours(16), d6.plusHours(22), d6.minusDays(2), 300, rahul, EventStatus.PUBLISHED,
                "Music, dance and drama competitions between 20 colleges, followed by a live band night.");
        session(fest, "Group dance finals", null, "Open Air Theatre", d6.plusHours(16).plusMinutes(30), 120);
        session(fest, "Battle of the bands", null, "Open Air Theatre", d6.plusHours(19), 150);
        for (int i = 0; i < 3; i++) {
            reg(fest, p.get(i + 8), RegistrationStatus.CONFIRMED, today.minusDays(4 - i).plusHours(20));
        }

        // ---------------------------------------------------------------- E7: cancelled webinar
        LocalDateTime d7 = today.plusDays(8);
        Event webinar = event("AI in Healthcare Webinar", EventCategory.WEBINAR, "Online (Zoom)",
                d7.plusHours(17), d7.plusHours(18), d7.minusDays(1), 500, priya, EventStatus.CANCELLED,
                "Cancelled due to speaker unavailability. A new date will be announced.");
        session(webinar, "AI-assisted diagnostics", kavita, "Online", d7.plusHours(17), 60);
    }

    // ---------------------------------------------------------------- helpers

    private User user(String name, String email, String password, Role role, String organization) {
        User u = new User(name, email, passwordEncoder.encode(password), role);
        u.setOrganization(organization);
        return users.save(u);
    }

    private Event event(String title, EventCategory category, String venue, LocalDateTime start, LocalDateTime end,
                        LocalDateTime deadline, int capacity, User organizer, EventStatus status, String description) {
        Event e = new Event(title, description, category, venue, start, end, deadline, capacity, organizer);
        e.setStatus(status);
        return events.save(e);
    }

    private EventSession session(Event event, String title, Speaker speaker, String room, LocalDateTime start, int minutes) {
        EventSession s = new EventSession(event, title, null, speaker, room, start, start.plusMinutes(minutes));
        event.getSessions().add(s);
        return sessions.save(s);
    }

    private Registration reg(Event event, User participant, RegistrationStatus status, LocalDateTime at) {
        return registrations.save(new Registration(event, participant, status, ticketCodes.newCode(), at));
    }

    private void attend(Registration r, EventSession s, LocalDateTime at) {
        attendance.save(new Attendance(r, s, at, Attendance.Method.QR));
    }
}
