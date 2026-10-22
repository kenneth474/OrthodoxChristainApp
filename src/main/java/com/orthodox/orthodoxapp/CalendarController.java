package com.orthodox.orthodoxapp;

import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CalendarController: the web endpoints for the liturgical calendar.
 *
 * A controller's only job is to handle web requests: read what was asked for,
 * call the right service, and return the result. It holds no business logic;
 * that lives in CalendarService.
 *
 * @RestController = "this class answers web requests and returns JSON".
 * @RequestMapping("/api") puts "/api" in front of every path in this class,
 * giving us two short, friendly addresses:
 *   GET /api/today              -> today's day
 *   GET /api/day/2026/12/25     -> any date
 */
@RestController
@RequestMapping("/api")
public class CalendarController {

    private final CalendarService calendarService;

    // Spring passes in the shared CalendarService (dependency injection).
    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /**
     * GET /api/today
     * Returns today's liturgical day.
     */
    @GetMapping("/today")
    public LiturgicalDay today() {
        return calendarService.getDay(calendarService.today());
    }

    /**
     * GET /api/day/{year}/{month}/{day}, e.g. /api/day/2026/12/25
     *
     * @PathVariable takes a piece of the address and passes it in as a
     * method parameter. Spring converts the text "2026" into the int 2026.
     *
     * LocalDate.of(...) throws a DateTimeException for impossible dates such
     * as February 30. ApiExceptionHandler turns that into a "400 Bad Request".
     */
    @GetMapping("/day/{year}/{month}/{day}")
    public LiturgicalDay day(@PathVariable int year,
                             @PathVariable int month,
                             @PathVariable int day) {
        LocalDate date = LocalDate.of(year, month, day);
        return calendarService.getDay(date);
    }
}
