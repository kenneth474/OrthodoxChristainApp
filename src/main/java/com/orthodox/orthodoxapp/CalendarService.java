package com.orthodox.orthodoxapp;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * CalendarService: the "business logic" for the liturgical calendar.
 *
 * It sits between the controller (web requests) and the client (Orthocal):
 *   1. asks OrthocalClient for the raw data,
 *   2. converts it into our own clean LiturgicalDay format,
 *   3. applies our rules (e.g. turning fasting codes into a FastingRule).
 *
 * @Service works like @Component (Spring creates one shared instance); the
 * different name just documents that this class holds business logic.
 */
@Service
public class CalendarService {

    // The parish time zone. "Today" means today in US Central time,
    // not today wherever the server happens to run.
    private static final ZoneId PARISH_TIME_ZONE = ZoneId.of("America/Chicago");

    private final OrthocalClient orthocalClient;

    /**
     * DEPENDENCY INJECTION
     * We never write "new OrthocalClient()". Instead, we ask for one in the
     * constructor, and Spring passes in the shared instance it created.
     * This makes classes easy to test (we could pass in a fake client) and
     * keeps them loosely connected.
     */
    public CalendarService(OrthocalClient orthocalClient) {
        this.orthocalClient = orthocalClient;
    }

    /** Today's date in the parish time zone. */
    public LocalDate today() {
        return LocalDate.now(PARISH_TIME_ZONE);
    }

    /**
     * Gets the liturgical day for any date.
     *
     * @param date the day to look up
     * @return the day in our own format
     */
    public LiturgicalDay getDay(LocalDate date) {
        OrthocalDay raw = orthocalClient.fetchDay(date);
        return convert(date, raw);
    }

    // ------------------------------------------------------------------
    // Conversion from Orthocal's format to ours.
    // "private" = only used inside this class.
    // ------------------------------------------------------------------

    private LiturgicalDay convert(LocalDate date, OrthocalDay raw) {

        // Work out the clear fasting rule from Orthocal's two numbers.
        FastingRule rule = FastingRule.from(raw.fastLevel(), raw.fastException());

        // Only show a season name on actual fasting days
        // (Orthocal says "No Fast" otherwise, which we don't need to repeat).
        String season = raw.fastLevel() == 0 ? "" : raw.fastLevelDesc();

        LiturgicalDay.Fasting fasting = new LiturgicalDay.Fasting(
                rule,
                rule.label(),
                season,
                // Prefer Orthocal's own list for the day; fall back to the
                // rule's standard list if Orthocal didn't send one.
                orEmpty(raw.fastAbstentions()).isEmpty() ? rule.abstainFrom() : raw.fastAbstentions());

        LiturgicalDay.Feast feast = new LiturgicalDay.Feast(
                raw.feastLevel(),
                raw.feastLevelDesc(),
                orEmpty(raw.feasts()));

        // Keep only the readings the Archdiocese lists for the day (see
        // ReadingSelector for the rules), then convert them.
        // STREAMS: a compact way to transform every item in a list.
        //   .stream()      -> start processing the list item by item
        //   .map(...)      -> convert each item into something else
        //   .toList()      -> collect the results into a new list
        // "this::convertReading" means "call convertReading on each item".
        List<LiturgicalDay.Reading> readings = ReadingSelector
                .select(orEmpty(raw.readings()), date, raw.paschaDistance(), raw.feastLevel())
                .stream()
                .map(this::convertReading)
                .toList();

        List<LiturgicalDay.Life> lives = orEmpty(raw.stories()).stream()
                // A "lambda": story -> new Life(...) is a tiny unnamed function.
                .map(story -> new LiturgicalDay.Life(story.title(), story.story()))
                .toList();

        return new LiturgicalDay(
                date,
                raw.summaryTitle(),
                orEmpty(raw.titles()),
                feast,
                fasting,
                raw.tone(),
                orEmpty(raw.saints()),
                readings,
                lives);
    }

    private LiturgicalDay.Reading convertReading(OrthocalDay.Reading reading) {
        List<LiturgicalDay.Verse> verses = orEmpty(reading.passage()).stream()
                .map(v -> new LiturgicalDay.Verse(v.chapter(), v.verse(), v.content(), v.paragraphStart()))
                .toList();

        ScriptureBooks.Parts parts = ScriptureBooks.split(reading.display());
        return new LiturgicalDay.Reading(
                reading.source(),
                parts.book(),
                parts.chapters(),
                ScriptureBooks.shortReference(reading.display()),
                reading.description() == null ? "" : reading.description(),
                verses);
    }

    /**
     * Returns the list unchanged, or an empty list if it is null.
     *
     * GENERICS: <T> means "works for a list of any type", so the same helper
     * handles List<String>, List<Reading>, and so on.
     */
    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
