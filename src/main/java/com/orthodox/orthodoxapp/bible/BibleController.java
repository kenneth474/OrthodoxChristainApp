package com.orthodox.orthodoxapp.bible;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * BibleController: the Bible tab's web endpoints. Anyone may read the Bible,
 * signed in or not (see SecurityConfig).
 *
 *   GET /api/bible/books        -> every book, in Orthodox order
 *   GET /api/bible/PSA/50       -> one chapter (here Psalm 50), with links to
 *                                  the chapters before and after
 */
@RestController
@RequestMapping("/api/bible")
public class BibleController {

    private final BibleService bible;

    public BibleController(BibleService bible) {
        this.bible = bible;
    }

    @GetMapping("/books")
    public List<BibleBook> books() {
        return bible.books();
    }

    /**
     * ResponseStatusException is Spring's quick way to answer with an error
     * status: here 404 "Not Found" for a book or chapter that doesn't exist.
     */
    @GetMapping("/{code}/{chapter}")
    public BibleChapter chapter(@PathVariable String code, @PathVariable int chapter) {
        return bible.chapter(code, chapter)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "That chapter doesn't exist."));
    }
}
