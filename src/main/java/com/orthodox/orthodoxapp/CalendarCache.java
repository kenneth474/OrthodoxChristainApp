package com.orthodox.orthodoxapp;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * CalendarCache: remembers each day's calendar after we've asked Orthocal
 * for it once, so the next visitor gets it instantly and Orthocal (a free
 * service run by one volunteer) isn't asked the same question again and again.
 *
 * HOW LONG IS A DAY KEPT?
 * Until the next midnight, Central time. A day's saints and readings don't
 * change, but if Orthocal ever corrects something we'll pick it up the
 * following day.
 *
 * WHAT IF ORTHOCAL IS DOWN?
 * An expired copy isn't thrown away straight away. CalendarService uses it
 * (stale(...)) when Orthocal can't be reached, because yesterday's copy of
 * today's saints is far better than an error message.
 *
 * WHERE IS IT KEPT?
 * In the app's memory (a Map). Simple and fast, and it needs no extra
 * service. The catch: it's emptied whenever the app restarts, which on
 * Render's free plan is after 15 quiet minutes. (The roadmap first said
 * Redis, a separate cache server; for one copy of the app, memory does the
 * same job without another service to run and pay for.)
 */
@Component
public class CalendarCache {

    /** At most this many days are remembered: more than a year's worth. */
    static final int MAX_DAYS = 400;

    private static final ZoneId PARISH_TIME_ZONE = ZoneId.of("America/Chicago");

    /** One remembered day, and the moment it stops being fresh. */
    private record Entry(LiturgicalDay day, Instant freshUntil) {
    }

    private final Clock clock;

    /**
     * LinkedHashMap with "true" keeps the days in order of last use, and
     * removeEldestEntry drops the least recently used day once there are more
     * than MAX_DAYS, so memory use stays small however many dates are visited.
     */
    private final Map<LocalDate, Entry> entries = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<LocalDate, Entry> eldest) {
            return size() > MAX_DAYS;
        }
    };

    /** The one Spring uses: the real clock. */
    public CalendarCache() {
        this(Clock.systemUTC());
    }

    /** For tests, which pass in a clock they control. */
    CalendarCache(Clock clock) {
        this.clock = clock;
    }

    /**
     * The day, if we have a copy that's still fresh.
     *
     * "synchronized" lets only one request at a time inside, because several
     * visitors can arrive at once and a plain Map isn't safe for that.
     */
    public synchronized Optional<LiturgicalDay> fresh(LocalDate date) {
        Entry entry = entries.get(date);
        if (entry == null || !clock.instant().isBefore(entry.freshUntil())) {
            return Optional.empty();
        }
        return Optional.of(entry.day());
    }

    /** The day even if its copy has expired: for when Orthocal can't be reached. */
    public synchronized Optional<LiturgicalDay> stale(LocalDate date) {
        Entry entry = entries.get(date);
        return entry == null ? Optional.empty() : Optional.of(entry.day());
    }

    /** Remembers a day until the next midnight, Central time. */
    public synchronized void put(LocalDate date, LiturgicalDay day) {
        Instant nextMidnight = LocalDate.now(clock.withZone(PARISH_TIME_ZONE))
                .plusDays(1)
                .atStartOfDay(PARISH_TIME_ZONE)
                .toInstant();
        entries.put(date, new Entry(day, nextMidnight));
    }

    /** How many days are remembered (for tests). */
    synchronized int size() {
        return entries.size();
    }
}
