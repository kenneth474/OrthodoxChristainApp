package com.orthodox.orthodoxapp;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ScriptureBooks: writes scripture references the way the Archdiocese does.
 *
 * Orthocal writes "Galatians 1.11-19"; antiochian.org writes
 * "St. Paul's Letter to the Galatians 1:11-19". This class splits a reference
 * into the book and the chapter-and-verse part, gives epistles their full
 * titles, and uses ":" between chapter and verse.
 */
final class ScriptureBooks {

    /** A reference split in two, e.g. book "St. Paul's Letter to the Galatians", chapters "1:11-19". */
    record Parts(String book, String chapters) {
    }

    // Map.ofEntries builds a fixed lookup table: short name -> full title.
    private static final Map<String, String> TITLES = Map.ofEntries(
            Map.entry("Acts", "The Acts of the Apostles"),
            Map.entry("Romans", "St. Paul's Letter to the Romans"),
            Map.entry("1 Corinthians", "St. Paul's First Letter to the Corinthians"),
            Map.entry("2 Corinthians", "St. Paul's Second Letter to the Corinthians"),
            Map.entry("Galatians", "St. Paul's Letter to the Galatians"),
            Map.entry("Ephesians", "St. Paul's Letter to the Ephesians"),
            Map.entry("Philippians", "St. Paul's Letter to the Philippians"),
            Map.entry("Colossians", "St. Paul's Letter to the Colossians"),
            Map.entry("1 Thessalonians", "St. Paul's First Letter to the Thessalonians"),
            Map.entry("2 Thessalonians", "St. Paul's Second Letter to the Thessalonians"),
            Map.entry("1 Timothy", "St. Paul's First Letter to Timothy"),
            Map.entry("2 Timothy", "St. Paul's Second Letter to Timothy"),
            Map.entry("Titus", "St. Paul's Letter to Titus"),
            Map.entry("Philemon", "St. Paul's Letter to Philemon"),
            Map.entry("Hebrews", "St. Paul's Letter to the Hebrews"),
            Map.entry("James", "The Catholic Letter of St. James"),
            Map.entry("1 Peter", "The First Catholic Letter of St. Peter"),
            Map.entry("2 Peter", "The Second Catholic Letter of St. Peter"),
            Map.entry("1 John", "The First Catholic Letter of St. John"),
            Map.entry("2 John", "The Second Catholic Letter of St. John"),
            Map.entry("3 John", "The Third Catholic Letter of St. John"),
            Map.entry("Jude", "The Catholic Letter of St. Jude"));

    // "1 Corinthians 12.27-13.8" -> book "1 Corinthians", chapters "12.27-13.8".
    // (.+?) is the shortest book name that still leaves only numbers and
    // punctuation after it; the [​] part removes an invisible character
    // Orthocal sometimes puts at the start.
    private static final Pattern REFERENCE = Pattern.compile("^​?(.+?)\\s+(\\d[\\d.:,;\\s-]*)$");

    private ScriptureBooks() {
    }

    static Parts split(String display) {
        String text = display == null ? "" : display.strip();
        Matcher m = REFERENCE.matcher(text);
        if (!m.matches()) {
            return new Parts(text, "");
        }
        String book = m.group(1);
        // "1.11-19" -> "1:11-19": a dot between two numbers means chapter:verse.
        String chapters = m.group(2).replaceAll("(\\d+)\\.(\\d+)", "$1:$2");
        return new Parts(TITLES.getOrDefault(book, book), chapters);
    }

    /** "Galatians 1.11-19" -> "Galatians 1:11-19" (the short form, with ":"). */
    static String shortReference(String display) {
        return display == null ? "" : display.strip().replace("​", "").replaceAll("(\\d+)\\.(\\d+)", "$1:$2");
    }
}
