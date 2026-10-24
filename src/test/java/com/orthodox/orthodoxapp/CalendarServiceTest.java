package com.orthodox.orthodoxapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

/**
 * Checks the calendar cache: each day is fetched from Orthocal once, kept
 * until midnight Central time, and an old copy is used if Orthocal is down.
 *
 * No real Orthocal and no real clock: FakeOrthocal counts its calls (and can
 * pretend to be offline), and TestClock is a clock we move by hand.
 */
class CalendarServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 23);

    /** A clock we can move forward ourselves. */
    static class TestClock extends Clock {
        Instant now;

        TestClock(String instant) {
            now = Instant.parse(instant);
        }

        @Override public Instant instant() { return now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
    }

    /** Stands in for OrthocalClient: counts calls, and can be "offline". */
    static class FakeOrthocal extends OrthocalClient {
        int calls;
        boolean offline;

        @Override
        public OrthocalDay fetchDay(LocalDate date) {
            calls++;
            if (offline) {
                throw new ResourceAccessException("Orthocal is offline");
            }
            return new OrthocalDay(List.of(), "Holy Apostle James", 0, "", List.of(),
                    0, "No Fast", 0, "", List.of(), 6, 0, List.of(), List.of(), List.of());
        }
    }

    // 3 p.m. Central time on October 23 (20:00 in UTC).
    private final TestClock clock = new TestClock("2026-10-23T20:00:00Z");
    private final FakeOrthocal orthocal = new FakeOrthocal();
    private final CalendarService service = new CalendarService(orthocal, new CalendarCache(clock));

    @Test
    void asksOrthocalOnlyOncePerDay() {
        LiturgicalDay first = service.getDay(DAY);
        LiturgicalDay second = service.getDay(DAY);

        assertThat(orthocal.calls).isEqualTo(1);
        assertThat(second).isSameAs(first);
        assertThat(second.title()).isEqualTo("Holy Apostle James");
    }

    @Test
    void asksAgainAfterMidnightCentralTime() {
        service.getDay(DAY);

        clock.now = Instant.parse("2026-10-24T04:59:00Z"); // 11:59 p.m. Central: still fresh
        service.getDay(DAY);
        assertThat(orthocal.calls).isEqualTo(1);

        clock.now = Instant.parse("2026-10-24T05:01:00Z"); // 12:01 a.m. Central: a new day
        service.getDay(DAY);
        assertThat(orthocal.calls).isEqualTo(2);
    }

    @Test
    void usesTheOldCopyWhenOrthocalIsDown() {
        service.getDay(DAY);
        clock.now = Instant.parse("2026-10-25T12:00:00Z"); // the copy has expired
        orthocal.offline = true;

        assertThat(service.getDay(DAY).title()).isEqualTo("Holy Apostle James");
        assertThat(orthocal.calls).isEqualTo(2); // it did try Orthocal first
    }

    @Test
    void passesTheErrorOnWhenThereIsNoCopy() {
        orthocal.offline = true;

        assertThatThrownBy(() -> service.getDay(DAY)).isInstanceOf(ResourceAccessException.class);
    }

    @Test
    void remembersAtMostMaxDays() {
        CalendarCache cache = new CalendarCache(clock);
        LiturgicalDay day = service.getDay(DAY);
        for (int i = 0; i < CalendarCache.MAX_DAYS + 50; i++) {
            cache.put(DAY.plusDays(i), day);
        }

        assertThat(cache.size()).isEqualTo(CalendarCache.MAX_DAYS);
        assertThat(cache.stale(DAY)).isEmpty(); // the oldest days were dropped
        assertThat(cache.stale(DAY.plusDays(CalendarCache.MAX_DAYS + 49))).isPresent();
    }
}
