package com.orthodox.orthodoxapp.bible;

/**
 * BibleBook: one book of the Bible, as listed on the Bible tab.
 *
 * @param code     a short code used in addresses, e.g. "PSA" in /api/bible/PSA/50
 * @param name     the name shown to people, e.g. "Psalms" or "1 Kingdoms (1 Samuel)"
 * @param section  the group it's listed under, e.g. "Law", "Gospels"
 * @param chapters how many chapters it has
 */
public record BibleBook(String code, String name, String section, int chapters) {
}
