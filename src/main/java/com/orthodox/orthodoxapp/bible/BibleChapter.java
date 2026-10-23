package com.orthodox.orthodoxapp.bible;

import java.util.List;

/**
 * BibleChapter: one chapter, as sent to the Bible tab.
 *
 * @param code     the book's code, e.g. "JOH"
 * @param book     the book's name, e.g. "John"
 * @param chapter  this chapter's number
 * @param chapters how many chapters the book has
 * @param verses   the text, verse by verse
 * @param previous the chapter before this one (it may be in the previous book), or null at the very start
 * @param next     the chapter after this one, or null at the very end
 */
public record BibleChapter(String code, String book, int chapter, int chapters, List<Verse> verses,
                           Place previous, Place next) {

    /**
     * One verse. The number is text because a few verses have letters,
     * e.g. Exodus 28:29a in the Septuagint. paragraphStart is true where the
     * King James text marks a new paragraph (with a ¶ sign in the file).
     */
    public record Verse(String number, String text, boolean paragraphStart) {
    }

    /** A chapter to jump to, e.g. {"code": "MAT", "book": "Matthew", "chapter": 28}. */
    public record Place(String code, String book, int chapter) {
    }
}
