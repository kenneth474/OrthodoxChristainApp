package com.orthodox.orthodoxapp;

import java.time.Duration;
import java.time.LocalDate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OrthocalClient: the ONLY class that talks to the Orthocal website.
 *
 * WHY A SEPARATE CLASS?
 * Our backend is built in layers, each with one job:
 *
 *     Controller  ->  Service  ->  Client  ->  (Orthocal website)
 *     handles web     business     calls an
 *     requests        rules        outside API
 *
 * If we ever switch calendar providers, or add caching (Redis, coming soon),
 * we only change this one class.
 *
 * @Component tells Spring "create one of these and share it with any class
 * that asks for it". That sharing is called dependency injection: see the
 * constructor of CalendarService for the other side.
 */
@Component
public class OrthocalClient {

    // How long to wait before giving up on Orthocal, so a slow or
    // offline website can't make our app hang forever.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    // The configured HTTP client, built once in the constructor below.
    private final RestClient restClient;

    /**
     * Constructor: Spring calls this once when the app starts.
     */
    public OrthocalClient() {
        // A "request factory" is the low-level piece that opens network
        // connections. We create one so we can set our timeouts on it.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        // The "builder" pattern: set options one at a time, then build().
        this.restClient = RestClient.builder()
                .baseUrl("https://orthocal.info")
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Fetches Orthocal's data for one date.
     *
     * The address has this shape: /api/{tradition}/{calendar}/{year}/{month}/{day}/
     *   - "greek" = Greek/Antiochian tradition (fasting rules, commemorations).
     *   - "gregorian" = the New (Revised Julian) calendar the Antiochian
     *     Archdiocese uses, so fixed feasts match the civil date.
     *
     * If Orthocal is down or replies with an error, RestClient throws a
     * RestClientException. We don't catch it here: ApiExceptionHandler turns it
     * into a friendly error for the frontend.
     *
     * @param date the day to look up
     * @return Orthocal's data for that day
     */
    public OrthocalDay fetchDay(LocalDate date) {
        return restClient.get()
                // {year}, {month} and {day} are filled in, in order, by the values after it.
                .uri("/api/greek/gregorian/{year}/{month}/{day}/",
                        date.getYear(),
                        date.getMonthValue(),
                        date.getDayOfMonth())
                // Send the request and wait for the reply.
                .retrieve()
                // Convert the JSON reply into an OrthocalDay record.
                .body(OrthocalDay.class);
    }
}
