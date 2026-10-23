package com.orthodox.orthodoxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Each test is a real day: Orthocal's readings in, and the readings
 * antiochian.org lists for that day expected out. The Lent and Pascha days
 * follow the same Greek practice (they were checked against Orthocal's data,
 * as antiochian.org only lists the current year's days).
 */
class ReadingSelectorTest {

    @Test
    void aSaintsReadingsReplaceTheWeekdayOnes() {
        // Friday, Oct 23, 2026: St James, Brother of the Lord
        assertChosen(LocalDate.of(2026, 10, 23), 194, 3, List.of(
                r("Epistle", "", "Colossians 2.1-7"),
                r("Epistle", "St James", "Galatians 1.11-19"),
                r("Gospel", "", "Luke 10.1-15"),
                r("Gospel", "St James", "Matthew 13.54-58")),
                "Galatians 1.11-19", "Matthew 13.54-58");
    }

    @Test
    void vespersMatinsAndEarlierSaintsAreLeftOut() {
        // Monday, Oct 26, 2026: St Demetrius (also the Earthquake commemoration)
        assertChosen(LocalDate.of(2026, 10, 26), 197, 4, List.of(
                r("Vespers", "", "Jeremiah 2.2-12"),
                r("Matins Gospel", "", "Matthew 10.16-22"),
                r("Epistle", "", "Colossians 2.13-20"),
                r("Epistle", "Earthquake", "Hebrews 12.6-13, 25-27"),
                r("Epistle", "St Demetrius", "2 Timothy 2.1-10"),
                r("Gospel", "", "Luke 10.22-24"),
                r("Gospel", "Earthquake", "Matthew 8.23-27"),
                r("Gospel", "St Demetrius", "John 15.17-16.2")),
                "2 Timothy 2.1-10", "John 15.17-16.2");
    }

    @Test
    void aSaintWithOnlyAnEpistleKeepsTheWeekdayGospel() {
        // Saturday, Oct 31, 2026: Apostles Stachys, Amplias and others
        assertChosen(LocalDate.of(2026, 10, 31), 202, 0, List.of(
                r("Epistle", "", "2 Corinthians 5.1-10"),
                r("Epistle", "Apostles Stachys, Amplias, et al", "Romans 16.1-16"),
                r("Gospel", "", "Luke 8.16-21")),
                "Romans 16.1-16", "Luke 8.16-21");
    }

    @Test
    void memorialSaturdaysOutsideTheTwoSaturdaysOfSoulsUseTheDailyReadings() {
        // Saturday, Oct 24, 2026 ("Demetrius Saturday", a Slavic custom)
        assertChosen(LocalDate.of(2026, 10, 24), 195, 0, List.of(
                r("Epistle", "", "2 Corinthians 3.12-18"),
                r("Epistle", "Departed", "1 Thessalonians 4.13-17"),
                r("Gospel", "", "Luke 7.1-10"),
                r("Gospel", "Departed", "John 5.24-30")),
                "2 Corinthians 3.12-18", "Luke 7.1-10");
    }

    @Test
    void onSundaysTheSundayGospelStays() {
        // Sunday, Nov 1, 2026: Sts Cosmas and Damian (a full set of readings)
        assertChosen(LocalDate.of(2026, 11, 1), 203, 2, List.of(
                r("Matins Gospel", "", "John 21.15-25"),
                r("Epistle", "", "Galatians 6.11-18"),
                r("Epistle", "Cosmas & Damian", "1 Corinthians 12.27-13.8"),
                r("Gospel", "", "Luke 16.19-31"),
                r("Gospel", "Cosmas & Damian", "Matthew 10.1, 5-8")),
                "1 Corinthians 12.27-13.8", "Luke 16.19-31");

        // Sunday, Nov 8, 2026: Synaxis of the Archangels (the last saint wins)
        assertChosen(LocalDate.of(2026, 11, 8), 210, 5, List.of(
                r("Epistle", "", "Ephesians 2.4-10"),
                r("Epistle", "Unmercenaries", "1 Corinthians 12.27-13.8"),
                r("Epistle", "Angels", "Hebrews 2.2-10"),
                r("Gospel", "", "Luke 8.41-56"),
                r("Gospel", "Unmercenaries", "Matthew 10.1, 5-8"),
                r("Gospel", "Angels", "Luke 10.16-21")),
                "Hebrews 2.2-10", "Luke 8.41-56");
    }

    @Test
    void onSundaysASaintWithOnlyAnEpistleDoesNotReplaceTheSundayEpistle() {
        // Sunday, Nov 22, 2026: St Philemon
        assertChosen(LocalDate.of(2026, 11, 22), 224, 2, List.of(
                r("Epistle", "", "Ephesians 4.1-7"),
                r("Epistle", "St Philemon", "Philemon 1-25"),
                r("Gospel", "", "Luke 12.16-21")),
                "Ephesians 4.1-7", "Luke 12.16-21");
    }

    @Test
    void greatFeastsUseTheirOwnReadings() {
        // Friday, Dec 25, 2026: the Nativity of Christ
        assertChosen(LocalDate.of(2026, 12, 25), 257, 8, List.of(
                r("Matins Gospel", "", "Matthew 1.18-25"),
                r("Epistle", "Nativity", "Galatians 4.4-7"),
                r("Gospel", "Nativity", "Matthew 2.1-12")),
                "Galatians 4.4-7", "Matthew 2.1-12");
    }

    @Test
    void palmSundayAndPaschaComeBeforeAnySaint() {
        // Palm Sunday, Apr 25, 2027 (also St Mark)
        assertChosen(LocalDate.of(2027, 4, 25), -7, 8, List.of(
                r("Epistle", "", "Philippians 4.4-9"),
                r("Epistle", "St Mark", "1 Peter 5.6-14"),
                r("Gospel", "", "John 12.1-18"),
                r("Gospel", "St Mark", "Luke 10.16-21")),
                "Philippians 4.4-9", "John 12.1-18");

        // Pascha, May 2, 2027 (also the relics of St Athanasius)
        assertChosen(LocalDate.of(2027, 5, 2), 0, 8, List.of(
                r("Epistle", "", "Acts 1.1-8"),
                r("Epistle", "Removal of Relics of Athanasius", "Hebrews 13.7-16"),
                r("Gospel", "", "John 1.1-17"),
                r("Gospel", "Removal of Relics of Athanasius", "Matthew 5.14-19")),
                "Acts 1.1-8", "John 1.1-17");
    }

    @Test
    void theSaturdayOfSoulsBeforeLentReadsForTheDeparted() {
        // Meatfare Saturday, Mar 6, 2027
        assertChosen(LocalDate.of(2027, 3, 6), -57, 0, List.of(
                r("Epistle", "Departed", "1 Thessalonians 4.13-17"),
                r("Epistle", "", "1 Corinthians 10.23-28"),
                r("Gospel", "Departed", "John 5.24-30"),
                r("Gospel", "", "Luke 21.8-9, 25-27, 33-36")),
                "1 Thessalonians 4.13-17", "John 5.24-30");
    }

    @Test
    void lentenWeekdaysWithoutALiturgyKeepTheirReadings() {
        // Wednesday, Mar 17, 2027: the Hours and Vespers readings
        assertChosen(LocalDate.of(2027, 3, 17), -46, 3, List.of(
                r("6th Hour", "", "Isaiah 2.3-11"),
                r("Vespers", "", "Genesis 1.24-2.3"),
                r("Vespers", "", "Proverbs 2.1-22")),
                "Isaiah 2.3-11", "Genesis 1.24-2.3", "Proverbs 2.1-22");
    }

    // ----- Helpers -----

    private static OrthocalDay.Reading r(String source, String description, String display) {
        return new OrthocalDay.Reading(source, description, display, List.of());
    }

    private static void assertChosen(LocalDate date, int paschaDistance, int feastLevel,
                                     List<OrthocalDay.Reading> readings, String... expected) {
        List<String> chosen = ReadingSelector.select(readings, date, paschaDistance, feastLevel).stream()
                .map(OrthocalDay.Reading::display)
                .toList();
        assertEquals(List.of(expected), chosen);
    }
}
