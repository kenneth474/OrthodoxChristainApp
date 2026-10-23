package com.orthodox.orthodoxapp.bible;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * BibleService: the whole Bible, loaded into memory when the app starts.
 *
 * The text comes from two public-domain files in src/main/resources/bible/
 * (see the README.md there):
 *   - the Old Testament from the Septuagint (Brenton's translation), the Greek
 *     Old Testament the Orthodox Church reads, with its Psalm numbering
 *   - the New Testament from the King James Version, the same wording as the
 *     daily readings
 *
 * The whole Bible is about 5 MB of text, small enough to keep in memory, so
 * every chapter can be sent back instantly without a database.
 */
@Service
public class BibleService {

    // One line of the files: "PSA 50:3 Have mercy upon me, O God..."
    //   group 1 = book code, group 2 = chapter, group 3 = verse (may be "29a"), group 4 = text
    private static final Pattern LINE = Pattern.compile("^(\\S+) (\\d+):(\\S+) (.*)$");

    /**
     * Every book in the order of the Orthodox Bible (the order of the Orthodox
     * Study Bible), with the section it's listed under. The Septuagint names
     * the books of Samuel and Kings "1-4 Kingdoms"; the familiar names are added
     * in brackets. A book only appears if its text was found in the files.
     */
    private static final List<String[]> CATALOG = List.of(
            // { code, name, section }
            new String[] {"GEN", "Genesis", "Law"},
            new String[] {"EXO", "Exodus", "Law"},
            new String[] {"LEV", "Leviticus", "Law"},
            new String[] {"NUM", "Numbers", "Law"},
            new String[] {"DEU", "Deuteronomy", "Law"},
            new String[] {"JOS", "Joshua", "History"},
            new String[] {"JDG", "Judges", "History"},
            new String[] {"RUT", "Ruth", "History"},
            new String[] {"1SA", "1 Kingdoms (1 Samuel)", "History"},
            new String[] {"2SA", "2 Kingdoms (2 Samuel)", "History"},
            new String[] {"1KI", "3 Kingdoms (1 Kings)", "History"},
            new String[] {"2KI", "4 Kingdoms (2 Kings)", "History"},
            new String[] {"1CH", "1 Chronicles", "History"},
            new String[] {"2CH", "2 Chronicles", "History"},
            new String[] {"1ES", "1 Esdras", "History"},
            new String[] {"EZR", "2 Esdras (Ezra and Nehemiah)", "History"},
            new String[] {"TOB", "Tobit", "History"},
            new String[] {"JDT", "Judith", "History"},
            new String[] {"ESG", "Esther", "History"},
            new String[] {"1MA", "1 Maccabees", "History"},
            new String[] {"2MA", "2 Maccabees", "History"},
            new String[] {"3MA", "3 Maccabees", "History"},
            new String[] {"PSA", "Psalms", "Wisdom"},
            new String[] {"JOB", "Job", "Wisdom"},
            new String[] {"PRO", "Proverbs", "Wisdom"},
            new String[] {"ECC", "Ecclesiastes", "Wisdom"},
            new String[] {"SOL", "Song of Songs", "Wisdom"},
            new String[] {"WIS", "Wisdom of Solomon", "Wisdom"},
            new String[] {"SIR", "Wisdom of Sirach", "Wisdom"},
            new String[] {"HOS", "Hosea", "Prophets"},
            new String[] {"AMO", "Amos", "Prophets"},
            new String[] {"MIC", "Micah", "Prophets"},
            new String[] {"JOE", "Joel", "Prophets"},
            new String[] {"OBA", "Obadiah", "Prophets"},
            new String[] {"JON", "Jonah", "Prophets"},
            new String[] {"NAH", "Nahum", "Prophets"},
            new String[] {"HAB", "Habakkuk", "Prophets"},
            new String[] {"ZEP", "Zephaniah", "Prophets"},
            new String[] {"HAG", "Haggai", "Prophets"},
            new String[] {"ZEC", "Zechariah", "Prophets"},
            new String[] {"MAL", "Malachi", "Prophets"},
            new String[] {"ISA", "Isaiah", "Prophets"},
            new String[] {"JER", "Jeremiah", "Prophets"},
            new String[] {"BAR", "Baruch", "Prophets"},
            new String[] {"LAM", "Lamentations", "Prophets"},
            new String[] {"EPJ", "Letter of Jeremiah", "Prophets"},
            new String[] {"EZE", "Ezekiel", "Prophets"},
            new String[] {"DNG", "Daniel", "Prophets"},
            new String[] {"SUS", "Susanna", "Prophets"},
            new String[] {"BEL", "Bel and the Dragon", "Prophets"},
            new String[] {"PRM", "Prayer of Manasseh", "Appendix"},
            new String[] {"4MA", "4 Maccabees", "Appendix"},
            new String[] {"MAT", "Matthew", "Gospels"},
            new String[] {"MAR", "Mark", "Gospels"},
            new String[] {"LUK", "Luke", "Gospels"},
            new String[] {"JOH", "John", "Gospels"},
            new String[] {"ACT", "Acts of the Apostles", "Acts and Epistles"},
            new String[] {"ROM", "Romans", "Acts and Epistles"},
            new String[] {"1CO", "1 Corinthians", "Acts and Epistles"},
            new String[] {"2CO", "2 Corinthians", "Acts and Epistles"},
            new String[] {"GAL", "Galatians", "Acts and Epistles"},
            new String[] {"EPH", "Ephesians", "Acts and Epistles"},
            new String[] {"PHI", "Philippians", "Acts and Epistles"},
            new String[] {"COL", "Colossians", "Acts and Epistles"},
            new String[] {"1TH", "1 Thessalonians", "Acts and Epistles"},
            new String[] {"2TH", "2 Thessalonians", "Acts and Epistles"},
            new String[] {"1TI", "1 Timothy", "Acts and Epistles"},
            new String[] {"2TI", "2 Timothy", "Acts and Epistles"},
            new String[] {"TIT", "Titus", "Acts and Epistles"},
            new String[] {"PHM", "Philemon", "Acts and Epistles"},
            new String[] {"HEB", "Hebrews", "Acts and Epistles"},
            new String[] {"JAM", "James", "Acts and Epistles"},
            new String[] {"1PE", "1 Peter", "Acts and Epistles"},
            new String[] {"2PE", "2 Peter", "Acts and Epistles"},
            new String[] {"1JO", "1 John", "Acts and Epistles"},
            new String[] {"2JO", "2 John", "Acts and Epistles"},
            new String[] {"3JO", "3 John", "Acts and Epistles"},
            new String[] {"JUD", "Jude", "Acts and Epistles"},
            new String[] {"REV", "Revelation", "Revelation"});

    // book code -> (chapter number -> verses). A TreeMap keeps chapters in order.
    private final Map<String, TreeMap<Integer, List<BibleChapter.Verse>>> text = new HashMap<>();
    private final List<BibleBook> books = new ArrayList<>();

    /** Spring creates this once at startup, which reads both files. */
    public BibleService() {
        load("bible/septuagint-brenton.txt");
        load("bible/kjv-new-testament.txt");
        for (String[] entry : CATALOG) {
            TreeMap<Integer, List<BibleChapter.Verse>> chapters = text.get(entry[0]);
            if (chapters != null) {
                books.add(new BibleBook(entry[0], entry[1], entry[2], chapters.lastKey()));
            }
        }
    }

    /** Every book, in Orthodox order. */
    public List<BibleBook> books() {
        return books;
    }

    /**
     * One chapter, or empty if the book or chapter doesn't exist.
     * Optional = "maybe a chapter, maybe nothing" (the controller turns
     * nothing into a 404 "Not Found").
     */
    public Optional<BibleChapter> chapter(String code, int chapter) {
        int index = indexOf(code);
        if (index < 0) {
            return Optional.empty();
        }
        BibleBook book = books.get(index);
        List<BibleChapter.Verse> verses = text.get(book.code()).get(chapter);
        if (verses == null) {
            return Optional.empty();
        }
        return Optional.of(new BibleChapter(book.code(), book.name(), chapter, book.chapters(), verses,
                previous(index, chapter), next(index, chapter)));
    }

    // ----- Helpers -----

    // Reads one file from src/main/resources, line by line.
    private void load(String path) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = LINE.matcher(line);
                if (!m.matches()) {
                    continue; // skip anything that isn't a verse
                }
                // "¶" at the start of a verse marks a new paragraph: remember it, and take it out of the text.
                String words = m.group(4).strip();
                boolean paragraph = words.startsWith("¶");
                if (paragraph) {
                    words = words.substring(1).strip();
                }
                // computeIfAbsent = "get the entry, creating an empty one first if there isn't one yet".
                text.computeIfAbsent(m.group(1), code -> new TreeMap<>())
                        .computeIfAbsent(Integer.parseInt(m.group(2)), number -> new ArrayList<>())
                        .add(new BibleChapter.Verse(m.group(3), words, paragraph));
            }
        } catch (IOException ex) {
            // Unchecked: without its Bible text the app is broken, so stop at startup.
            throw new UncheckedIOException("Couldn't read the Bible text " + path, ex);
        }
    }

    private int indexOf(String code) {
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).code().equalsIgnoreCase(code)) {
                return i;
            }
        }
        return -1;
    }

    // The chapter before: earlier in this book, or the last chapter of the book before.
    private BibleChapter.Place previous(int index, int chapter) {
        if (chapter > 1) {
            return place(books.get(index), chapter - 1);
        }
        return index > 0 ? place(books.get(index - 1), books.get(index - 1).chapters()) : null;
    }

    // The chapter after: later in this book, or chapter 1 of the next book.
    private BibleChapter.Place next(int index, int chapter) {
        if (chapter < books.get(index).chapters()) {
            return place(books.get(index), chapter + 1);
        }
        return index < books.size() - 1 ? place(books.get(index + 1), 1) : null;
    }

    private static BibleChapter.Place place(BibleBook book, int chapter) {
        return new BibleChapter.Place(book.code(), book.name(), chapter);
    }
}
