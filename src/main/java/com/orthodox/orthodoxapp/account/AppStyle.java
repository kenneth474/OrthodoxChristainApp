package com.orthodox.orthodoxapp.account;

/**
 * The app's five looks ("styles"). Each has a light and a dark version, so
 * this works together with Theme (light, dark or match my device).
 *
 * The colors and fonts themselves live in the React app
 * (frontend/src/styles/theme.css); the backend only remembers which one
 * each person chose.
 */
public enum AppStyle {
    /** Parchment, red rubrics and gold, like a service book. The default. */
    ILLUMINATED,
    /** Deep lapis blue and gold, like a Byzantine mosaic. */
    MOSAIC,
    /** Candle gold on near-black, like a church at Vespers. */
    VIGIL,
    /** Stone, olive wood and terracotta, like a monastery on Mount Athos. */
    ATHOS,
    /** Follows the Church year, like the colors of the vestments. */
    SEASONS
}
