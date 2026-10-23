package com.orthodox.orthodoxapp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ReadingSelector: picks the day's readings the way the Antiochian
 * Archdiocese lists them (antiochian.org's daily readings come from the Greek
 * Orthodox Archdiocese's Online Chapel, which follows Greek practice).
 *
 * WHY IS THIS NEEDED?
 * Orthocal lists EVERY reading that could be read that day: Vespers readings,
 * the Matins Gospel, the ordinary weekday readings AND the saint's readings.
 * The Archdiocese shows just the Divine Liturgy's Epistle and Gospel, chosen
 * by these rules:
 *
 *   1. Only the Liturgy's Epistle and Gospel.
 *   2. A saint's or feast's own readings replace the weekday ones. If several
 *      saints have readings, the last one Orthocal lists is the one used.
 *   3. On Sundays the Sunday Gospel stays. The saint's Epistle is read only
 *      when the saint has a full set of readings (Epistle AND Gospel), or the
 *      feast is a Great Feast.
 *   4. "Departed" readings are used only on the two Saturdays of Souls (before
 *      Lent and before Pentecost); other memorial Saturdays are a Slavic custom.
 *   5. From Palm Sunday to Thomas Sunday, and on Ascension, Pentecost and the
 *      Monday of the Holy Spirit, the day's own readings always come first.
 *   6. Days with no Liturgy readings (e.g. weekdays of Great Lent) keep all
 *      their readings (the Hours and Vespers readings).
 *
 * These rules were checked against 47 days listed on antiochian.org
 * (Oct 23 to Dec 8, 2026); ReadingSelectorTest repeats a selection of them.
 */
final class ReadingSelector {

    // Days counted from Pascha: Saturday before Lent (Meatfare) and before Pentecost.
    private static final Set<Integer> SATURDAYS_OF_SOULS = Set.of(-57, 48);
    // Ascension, Pentecost, Monday of the Holy Spirit.
    private static final Set<Integer> GREAT_MOVABLE_FEASTS = Set.of(39, 49, 50);
    private static final int GREAT_FEAST_LEVEL = 7;

    private ReadingSelector() {
    }

    static List<OrthocalDay.Reading> select(List<OrthocalDay.Reading> all, LocalDate date,
                                            int paschaDistance, int feastLevel) {
        List<OrthocalDay.Reading> epistles = ofService(all, "Epistle");
        List<OrthocalDay.Reading> gospels = ofService(all, "Gospel");
        if (epistles.isEmpty() && gospels.isEmpty()) {
            return all; // rule 6
        }

        OrthocalDay.Reading epistle;
        OrthocalDay.Reading gospel;

        if (SATURDAYS_OF_SOULS.contains(paschaDistance)) {                       // rule 4
            epistle = firstNonNull(departed(epistles), dayOwn(epistles));
            gospel = firstNonNull(departed(gospels), dayOwn(gospels));
        } else if ((paschaDistance >= -7 && paschaDistance <= 7)
                || GREAT_MOVABLE_FEASTS.contains(paschaDistance)) {                // rule 5
            epistle = firstNonNull(dayOwn(epistles), lastSaint(epistles));
            gospel = firstNonNull(dayOwn(gospels), lastSaint(gospels));
        } else {
            OrthocalDay.Reading saintEpistle = lastSaint(epistles);
            OrthocalDay.Reading saintGospel = lastSaint(gospels);
            boolean sunday = date.getDayOfWeek() == DayOfWeek.SUNDAY;

            if (sunday && feastLevel < GREAT_FEAST_LEVEL) {                        // rule 3
                epistle = (saintEpistle != null && saintGospel != null) ? saintEpistle : dayOwn(epistles);
                gospel = dayOwn(gospels);
            } else {                                                                // rule 2
                epistle = saintEpistle;
                gospel = saintGospel;
            }
            epistle = firstNonNull(epistle, dayOwn(epistles), saintEpistle);
            gospel = firstNonNull(gospel, dayOwn(gospels), saintGospel);
        }

        List<OrthocalDay.Reading> chosen = new ArrayList<>();
        if (epistle != null) chosen.add(epistle);
        if (gospel != null) chosen.add(gospel);
        return chosen;
    }

    // ----- Helpers -----

    // Rule 1: only readings for this service ("Epistle" or "Gospel").
    private static List<OrthocalDay.Reading> ofService(List<OrthocalDay.Reading> all, String service) {
        return all.stream().filter(r -> service.equals(r.source())).toList();
    }

    // The ordinary reading for the day: it has no description.
    private static OrthocalDay.Reading dayOwn(List<OrthocalDay.Reading> readings) {
        return readings.stream().filter(r -> isBlank(r.description())).findFirst().orElse(null);
    }

    // The last saint's or feast's reading (never the "Departed" ones).
    private static OrthocalDay.Reading lastSaint(List<OrthocalDay.Reading> readings) {
        OrthocalDay.Reading last = null;
        for (OrthocalDay.Reading r : readings) {
            if (!isBlank(r.description()) && !r.description().startsWith("Departed")) {
                last = r;
            }
        }
        return last;
    }

    // The readings for the departed (not the "Departed, variant" alternatives).
    private static OrthocalDay.Reading departed(List<OrthocalDay.Reading> readings) {
        return readings.stream().filter(r -> "Departed".equals(r.description())).findFirst().orElse(null);
    }

    // "..." = any number of arguments, which arrive as an array.
    @SafeVarargs
    private static <T> T firstNonNull(T... options) {
        for (T option : options) {
            if (option != null) {
                return option;
            }
        }
        return null;
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
