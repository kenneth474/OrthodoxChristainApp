package com.orthodox.orthodoxapp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * OrthocalDay is a mirror of the JSON the Orthocal API sends for ONE day.
 *
 * ROLE IN THE APP
 * This record is only used INSIDE the backend, to receive Orthocal's data.
 * We never send it straight to the frontend any more. Instead, CalendarService
 * converts it into our own cleaner LiturgicalDay record. Keeping the two
 * separate means that if Orthocal ever renames a field, only this file and the
 * converter need to change; the frontend never notices.
 *
 * WHAT IS A "record"?
 * A record is a short way to write a class whose only job is to hold data.
 * Java automatically creates the constructor, the getter methods
 * (e.g. summaryTitle()), equals(), hashCode() and toString() for us.
 *
 * HOW DOES THE JSON GET INTO THIS RECORD?
 * Spring Boot includes Jackson, a library that converts JSON into Java objects.
 * The @JsonProperty("...") labels tell Jackson which JSON field goes into which
 * Java variable. Orthocal uses snake_case names (summary_title) while Java uses
 * camelCase (summaryTitle), so we spell out the mapping.
 * Orthocal sends more fields than we list here; Jackson ignores the extras.
 *
 * NOTE ON EMPTY LISTS: on days with nothing to list (e.g. no feasts), Orthocal
 * sends null instead of an empty list, so code using these lists must check
 * for null first. CalendarService does that for us.
 */
public record OrthocalDay(

        // Descriptive names for the day, e.g. ["Thursday of the 5th Week"].
        @JsonProperty("titles") List<String> titles,

        // The best single title for the day: the feast or the main saint.
        @JsonProperty("summary_title") String summaryTitle,

        // How important today's feast is, as a number (-1 to 8; higher = greater feast).
        @JsonProperty("feast_level") int feastLevel,

        // Human-readable version of feast_level, e.g. "Great Feast".
        @JsonProperty("feast_level_description") String feastLevelDesc,

        // Names of feasts celebrated today, if any.
        @JsonProperty("feasts") List<String> feasts,

        // Which fasting SEASON applies today:
        //   0 = No Fast, 1 = Fast (e.g. ordinary Wed/Fri), 2 = Great Lent,
        //   3 = Apostles' Fast, 4 = Dormition Fast, 5 = Nativity Fast.
        @JsonProperty("fast_level") int fastLevel,

        // Name of the fasting season, e.g. "Nativity Fast".
        @JsonProperty("fast_level_desc") String fastLevelDesc,

        // A code (0-11) saying how strict today's fast is. FastingRule.java
        // explains every code and turns it into a clear rule.
        @JsonProperty("fast_exception") int fastException,

        // Words for that code, e.g. "Wine and Oil are Allowed".
        @JsonProperty("fast_exception_desc") String fastExceptionDesc,

        // Foods to avoid today, e.g. ["meat", "dairy", "eggs"].
        @JsonProperty("fast_abstentions") List<String> fastAbstentions,

        // The tone (1-8) of the week, used in services.
        @JsonProperty("tone") int tone,

        // Days from this year's Pascha (Easter): 0 = Pascha, -7 = Palm Sunday,
        // 49 = Pentecost. ReadingSelector uses it to spot movable feasts.
        @JsonProperty("pascha_distance") int paschaDistance,

        // Saints commemorated today.
        @JsonProperty("saints") List<String> saints,

        // Today's scripture readings, including the full text of each verse.
        @JsonProperty("readings") List<Reading> readings,

        // Lives of the saints and feast stories ("stories" in Orthocal's words).
        @JsonProperty("stories") List<Story> stories
) {

    /**
     * One scripture reading.
     *
     * A "nested" record lives inside OrthocalDay because it only makes sense
     * as part of a day.
     */
    public record Reading(
            // Which service it is read at, e.g. "Liturgy" or "Vespers".
            @JsonProperty("source") String source,
            // Extra note, e.g. "Saint" or "Feast" (often empty).
            @JsonProperty("description") String description,
            // The reference, e.g. "Luke 9.49-56".
            @JsonProperty("display") String display,
            // Every verse of the passage, in order.
            @JsonProperty("passage") List<Verse> passage
    ) {
    }

    /** One verse of scripture. */
    public record Verse(
            @JsonProperty("chapter") int chapter,
            @JsonProperty("verse") int verse,
            // The words of the verse.
            @JsonProperty("content") String content,
            // True if a new paragraph starts at this verse.
            @JsonProperty("paragraph_start") boolean paragraphStart
    ) {
    }

    /** The life of a saint or the story of a feast. */
    public record Story(
            @JsonProperty("title") String title,
            // The story as HTML (it contains <p> paragraph tags and similar).
            @JsonProperty("story") String story
    ) {
    }
}
