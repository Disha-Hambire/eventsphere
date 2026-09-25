package com.eventsphere.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Human-friendly date formatting for business-rule messages. */
final class Fmt {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private Fmt() {
    }

    static String dateTime(LocalDateTime t) {
        return t == null ? "-" : t.format(DATE_TIME);
    }

    static String time(LocalDateTime t) {
        return t == null ? "-" : t.format(TIME);
    }
}
