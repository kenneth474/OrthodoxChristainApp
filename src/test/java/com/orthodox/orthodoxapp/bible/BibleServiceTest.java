package com.orthodox.orthodoxapp.bible;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Loads the real Bible files and checks a few things that matter for an
 * Orthodox Bible. BibleService is created directly with "new": it needs no
 * other parts of the app, so the test doesn't have to start Spring.
 */
class BibleServiceTest {

    private static BibleService bible;

    // @BeforeAll runs once before all the tests: loading the Bible takes a moment.
    @BeforeAll
    static void loadTheBible() {
        bible = new BibleService();
    }

    @Test
    void hasTheOrthodoxCanonInOrder() {
        List<String> codes = bible.books().stream().map(BibleBook::code).toList();
        assertEquals("GEN", codes.get(0));
        assertEquals("REV", codes.get(codes.size() - 1));
        // Books in the Orthodox Old Testament but not in most Protestant Bibles:
        assertTrue(codes.containsAll(List.of("TOB", "JDT", "WIS", "SIR", "BAR", "1MA", "2MA", "3MA")));
        assertEquals(79, codes.size(), "52 Old Testament (Septuagint) books + 27 New Testament books");
    }

    @Test
    void psalmsUseTheSeptuagintNumbering() {
        // Psalm 50 in Orthodox books is "Have mercy on me, O God" (Psalm 51 in most Western Bibles).
        BibleChapter psalm50 = bible.chapter("PSA", 50).orElseThrow();
        assertTrue(psalm50.verses().get(2).text().startsWith("Have mercy upon me, O God"));
        assertEquals(151, bible.chapter("PSA", 151).orElseThrow().chapters());
    }

    @Test
    void theNewTestamentIsComplete() {
        assertEquals(28, bible.chapter("MAT", 1).orElseThrow().chapters());
        BibleChapter.Verse john316 = bible.chapter("JOH", 3).orElseThrow().verses().get(15);
        assertTrue(john316.text().startsWith("For God so loved the world"), "the ¶ sign is removed");
        assertTrue(john316.paragraphStart(), "...and remembered as a new paragraph");
    }

    @Test
    void previousAndNextCrossBetweenBooks() {
        BibleChapter matthew1 = bible.chapter("MAT", 1).orElseThrow();
        assertEquals("4MA", matthew1.previous().code()); // the Old Testament's last book
        assertEquals(2, matthew1.next().chapter());
        assertNull(bible.chapter("GEN", 1).orElseThrow().previous());
        assertNull(bible.chapter("REV", 22).orElseThrow().next());
    }

    @Test
    void unknownBooksAndChaptersAreEmpty() {
        assertTrue(bible.chapter("XYZ", 1).isEmpty());
        assertTrue(bible.chapter("GEN", 51).isEmpty());
    }
}
