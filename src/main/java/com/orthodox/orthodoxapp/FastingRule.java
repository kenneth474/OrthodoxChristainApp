package com.orthodox.orthodoxapp;

import java.util.List;

/**
 * FastingRule: the fasting rule for a day, as one clear value.
 *
 * WHAT IS AN ENUM?
 * An enum is a type with a fixed list of possible values. A day's fasting rule
 * can only ever be one of the values below, so an enum is a perfect fit.
 * Using an enum instead of plain text means:
 *   - typos are impossible (FastingRule.STRCT will not compile),
 *   - the frontend gets a short, stable code like "WINE_AND_OIL" that it can
 *     use to choose colours and icons.
 *
 * ENUMS CAN HOLD DATA
 * Each value below is created with a label and a list of foods to avoid, like
 * calling a constructor. The fields and constructor are further down.
 *
 * The rules follow Orthocal's Greek/Antiochian data. Orthocal reflects strict
 * typikon practice; parishes may relax rules pastorally.
 */
public enum FastingRule {

    // ----- The possible values, from strictest to most relaxed -----

    STRICT("Strict fast",
            List.of("meat", "fish", "dairy", "eggs", "wine", "oil")),

    WINE_ONLY("Wine allowed",
            List.of("meat", "fish", "dairy", "eggs", "oil")),

    WINE_AND_OIL("Wine and oil allowed",
            List.of("meat", "fish", "dairy", "eggs")),

    FISH_WINE_AND_OIL("Fish, wine and oil allowed",
            List.of("meat", "dairy", "eggs")),

    // The week before Great Lent (Cheesefare): no meat, but dairy and eggs are fine.
    MEAT_FAST("No meat (dairy and eggs allowed)",
            List.of("meat")),

    // A "fast-free" period, e.g. Bright Week: even Wednesday and Friday are free.
    FAST_FREE("Fast-free",
            List.of()),

    // An ordinary day with no fast.
    NO_FAST("No fast",
            List.of());

    // ----- Data stored in each value -----

    // "final" means the value is set once (in the constructor) and never changes.
    private final String label;
    private final List<String> abstainFrom;

    /**
     * Enum constructor. Java calls it once for each value above, for example
     * STRICT("Strict fast", List.of(...)). Enum constructors are always private:
     * no other code can create new FastingRule values.
     */
    FastingRule(String label, List<String> abstainFrom) {
        this.label = label;
        this.abstainFrom = abstainFrom;
    }

    /** A friendly description to show users, e.g. "Wine and oil allowed". */
    public String label() {
        return label;
    }

    /** Foods to avoid under this rule. Empty when there's no fast. */
    public List<String> abstainFrom() {
        return abstainFrom;
    }

    /** True if this rule restricts any food at all. */
    public boolean isFasting() {
        return !abstainFrom.isEmpty();
    }

    /**
     * Works out the rule from Orthocal's two numbers.
     *
     * "static" means you call it on the enum itself, not on a value:
     *     FastingRule rule = FastingRule.from(day.fastLevel(), day.fastException());
     *
     * @param fastLevel     Orthocal's fasting season (0 = no fast)
     * @param fastException Orthocal's strictness code (0-11), meanings below
     */
    public static FastingRule from(int fastLevel, int fastException) {

        // Code 11 means "Fast Free" (e.g. Bright Week). Check it first, because
        // it can appear inside what would otherwise be a fasting period.
        if (fastException == 11) {
            return FAST_FREE;
        }

        // Season 0 means today is not a fasting day at all.
        if (fastLevel == 0) {
            return NO_FAST;
        }

        // Otherwise it's a fasting day; the exception code says how strict.
        // A "switch expression" picks one result based on the value.
        // Several codes share a meaning, so they're grouped with commas.
        return switch (fastException) {
            // 1, 3 = "Wine and Oil are Allowed"
            // 6    = "Wine, Oil and Caviar" (Lazarus Saturday)
            // 8    = "Strict Fast (Wine and Oil)" (e.g. Exaltation of the Cross)
            case 1, 3, 6, 8 -> WINE_AND_OIL;
            // 2, 4 = "Fish, Wine and Oil are Allowed"
            case 2, 4 -> FISH_WINE_AND_OIL;
            // 5 = "Wine is Allowed" (Holy Saturday: wine but no oil)
            case 5 -> WINE_ONLY;
            // 7 = "Meat Fast" (Cheesefare week)
            case 7 -> MEAT_FAST;
            // 0 = no exception, 9 = "Strict Fast", 10 = "No overrides".
            // "default" also catches any unknown future code; strict is the safe choice.
            default -> STRICT;
        };
    }
}
