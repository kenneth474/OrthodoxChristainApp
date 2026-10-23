package com.orthodox.orthodoxapp;

import java.time.LocalDate;
import java.util.List;

/**
 * LiturgicalDay: the data OUR backend sends to the frontend for one day.
 *
 * WHY NOT JUST SEND ORTHOCAL'S DATA?
 * OrthocalDay mirrors someone else's API, with their naming and quirks
 * (null lists, numeric codes). LiturgicalDay is designed for our app:
 *   - camelCase names the frontend can use directly,
 *   - never-null lists,
 *   - a FastingRule enum instead of mystery numbers,
 *   - grouped sections (fasting, readings, lives).
 * This kind of class is often called a DTO ("Data Transfer Object").
 *
 * There are no @JsonProperty labels here, so Jackson uses the Java names
 * as-is: the JSON will contain "title", "fasting", "readings" and so on.
 */
public record LiturgicalDay(

        // The date this data is for. Jackson writes it as text, e.g. "2026-10-22".
        LocalDate date,

        // Main title for the day (feast or main saint).
        String title,

        // Extra titles, e.g. ["Thursday of the 5th Week"].
        List<String> subtitles,

        // Feast information (see Feast below).
        Feast feast,

        // Fasting information (see Fasting below).
        Fasting fasting,

        // Tone of the week (1-8), or 0 if none.
        int tone,

        // Saints commemorated today.
        List<String> saints,

        // Scripture readings with full text.
        List<Reading> readings,

        // Lives of the saints and feast stories.
        List<Life> lives
) {

    /**
     * Feast details.
     *
     * @param level  importance from -1 to 8 (higher = greater feast)
     * @param rank   description of the level, e.g. "Great Feast"
     * @param names  names of the feasts celebrated today
     */
    public record Feast(int level, String rank, List<String> names) {
    }

    /**
     * Fasting details.
     *
     * @param rule         the clear rule, e.g. WINE_AND_OIL (sent as text in JSON)
     * @param label        friendly wording, e.g. "Wine and oil allowed"
     * @param season       the fasting season, e.g. "Nativity Fast" (empty if none)
     * @param abstainFrom  foods to avoid today
     */
    public record Fasting(FastingRule rule, String label, String season, List<String> abstainFrom) {
    }

    /**
     * One scripture reading.
     *
     * @param service    where it is read, e.g. "Epistle" or "Gospel"
     * @param book       the book's full title, as the Archdiocese writes it,
     *                   e.g. "St. Paul's Letter to the Galatians" or "Matthew"
     * @param chapters   chapter and verses, e.g. "1:11-19"
     * @param reference  the short form, e.g. "Galatians 1:11-19"
     * @param note       whose reading it is, e.g. "St James" (empty for the ordinary daily reading)
     * @param verses     the full text
     */
    public record Reading(String service, String book, String chapters, String reference,
                          String note, List<Verse> verses) {
    }

    /** One verse: its number and words, and whether a paragraph starts here. */
    public record Verse(int chapter, int number, String text, boolean paragraphStart) {
    }

    /**
     * A saint's life or a feast story.
     *
     * @param title  e.g. "St Abercius, Equal-to-the-Apostles"
     * @param html   the story as HTML paragraphs (the frontend cleans it before showing it)
     */
    public record Life(String title, String html) {
    }
}
