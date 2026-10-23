package com.orthodox.orthodoxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScriptureBooksTest {

    @Test
    void epistlesGetTheirFullTitleAndColons() {
        assertEquals(new ScriptureBooks.Parts("St. Paul's Letter to the Galatians", "1:11-19"),
                ScriptureBooks.split("Galatians 1.11-19"));
        assertEquals(new ScriptureBooks.Parts("St. Paul's First Letter to the Corinthians", "12:27-13:8"),
                ScriptureBooks.split("1 Corinthians 12.27-13.8"));
        assertEquals(new ScriptureBooks.Parts("St. Paul's Letter to Philemon", "1-25"),
                ScriptureBooks.split("Philemon 1-25"));
    }

    @Test
    void gospelsKeepTheirName() {
        assertEquals(new ScriptureBooks.Parts("Matthew", "10:1, 5-8"),
                ScriptureBooks.split("Matthew 10.1, 5-8"));
        assertEquals(new ScriptureBooks.Parts("Luke", "17:26-37; 18:8"),
                ScriptureBooks.split("Luke 17.26-37; 18.8"));
    }

    @Test
    void theShortReferenceUsesColons() {
        assertEquals("Colossians 2:20-3:3", ScriptureBooks.shortReference("Colossians 2.20-3.3"));
    }
}
