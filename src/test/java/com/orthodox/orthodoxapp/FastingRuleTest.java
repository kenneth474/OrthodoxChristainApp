package com.orthodox.orthodoxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * FastingRuleTest: automated checks for FastingRule.from(...).
 *
 * WHAT IS A TEST?
 * A test is a small method that runs our code with known inputs and checks the
 * answer. If someone changes FastingRule and breaks a rule, a test fails and
 * tells us exactly which one. GitHub Actions runs these on every push (see
 * .github/workflows/ci.yml), and you can run them in VS Code from the Testing
 * panel (the flask icon in the left sidebar).
 *
 * HOW TO READ ONE
 * assertEquals(expected, actual) means "these must be equal, or the test fails".
 * @Test marks a method as a test, so JUnit (the testing library) runs it.
 *
 * The numbers are Orthocal's codes: from(fastLevel, fastException).
 */
class FastingRuleTest {

    @Test
    void noFastingSeasonMeansNoFast() {
        assertEquals(FastingRule.NO_FAST, FastingRule.from(0, 0));
    }

    @Test
    void code11IsFastFreeEvenInsideAFastingSeason() {
        // Bright Week: code 11 wins over the fasting season.
        assertEquals(FastingRule.FAST_FREE, FastingRule.from(3, 11));
        assertEquals(FastingRule.FAST_FREE, FastingRule.from(0, 11));
    }

    @Test
    void wineAndOilCodes() {
        assertEquals(FastingRule.WINE_AND_OIL, FastingRule.from(1, 1));
        assertEquals(FastingRule.WINE_AND_OIL, FastingRule.from(1, 3));
        assertEquals(FastingRule.WINE_AND_OIL, FastingRule.from(1, 6)); // Lazarus Saturday
        assertEquals(FastingRule.WINE_AND_OIL, FastingRule.from(1, 8)); // Exaltation of the Cross
    }

    @Test
    void fishWineAndOilCodes() {
        assertEquals(FastingRule.FISH_WINE_AND_OIL, FastingRule.from(1, 2));
        assertEquals(FastingRule.FISH_WINE_AND_OIL, FastingRule.from(1, 4));
    }

    @Test
    void holySaturdayIsWineOnly() {
        assertEquals(FastingRule.WINE_ONLY, FastingRule.from(1, 5));
    }

    @Test
    void cheesefareIsMeatFast() {
        assertEquals(FastingRule.MEAT_FAST, FastingRule.from(1, 7));
    }

    @Test
    void strictCodesAndUnknownCodesAreStrict() {
        assertEquals(FastingRule.STRICT, FastingRule.from(1, 0));
        assertEquals(FastingRule.STRICT, FastingRule.from(1, 9));
        assertEquals(FastingRule.STRICT, FastingRule.from(1, 10));
        // A code Orthocal might add in future: strict is the safe choice.
        assertEquals(FastingRule.STRICT, FastingRule.from(1, 42));
    }

    @Test
    void isFastingMatchesTheFoodList() {
        assertTrue(FastingRule.STRICT.isFasting());
        assertTrue(FastingRule.MEAT_FAST.isFasting());
        assertFalse(FastingRule.NO_FAST.isFasting());
        assertFalse(FastingRule.FAST_FREE.isFasting());
    }

    @Test
    void wineAndOilStillAvoidsMeatFishDairyAndEggs() {
        assertEquals(
                java.util.List.of("meat", "fish", "dairy", "eggs"),
                FastingRule.WINE_AND_OIL.abstainFrom());
    }
}
