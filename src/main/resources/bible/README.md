# Bible text used by the Bible tab

Both texts are in the **public domain**, so the app may include and display them.

| File | What | Source | License |
|---|---|---|---|
| `septuagint-brenton.txt` | Old Testament: the **Septuagint**, the Greek Old Testament the Orthodox Church uses, in Sir Lancelot Brenton's English translation (1851), with the books the Orthodox Bible includes (Tobit, Judith, Wisdom, Sirach, Baruch, 1–4 Maccabees, 1 Esdras, the Prayer of Manasseh, Psalm 151...). Uses the **Septuagint Psalm numbering** that Orthodox service books use (Psalm 50 = "Have mercy on me, O God"). | "Updated Brenton English Septuagint" (`englxxup`) from [eBible.org](https://ebible.org/find/details.php?id=englxxup) | Public domain; spelling updates by Adam Boyd (2020) released under CC0 |
| `kjv-new-testament.txt` | New Testament: the **King James Version**, the same wording as the daily readings (which come from Orthocal) | "King James Version" (`eng-kjv2006`) from [eBible.org](https://ebible.org/find/details.php?id=eng-kjv2006); only the New Testament books are kept | Public domain outside the UK |

## Format

One verse per line: `BOOK chapter:verse text`, for example

```
PSA 50:3 Have mercy upon me, O God, according to thy great mercy; ...
JOH 3:16 For God so loved the world, ...
```

- `BOOK` is a three-letter code; `BibleService.java` lists every code with its
  full name and its place in the Orthodox order of books.
- A few verses have letters (`EXO 28:29a`), so verse numbers are kept as text.
- Words in `[square brackets]` were added by the translator to make the English
  read smoothly; the Bible page shows them in italics.
