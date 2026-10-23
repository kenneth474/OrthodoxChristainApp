package com.orthodox.orthodoxapp;

import com.orthodox.orthodoxapp.auth.AuthException;
import java.time.DateTimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

/**
 * ApiExceptionHandler: turns errors into clear JSON replies.
 *
 * Without this, an error anywhere in a controller would produce a generic
 * "500 Internal Server Error". With it, the frontend gets a proper status code
 * and a message it can show the user.
 *
 * @RestControllerAdvice = "apply the @ExceptionHandler methods below to EVERY
 * controller in the app". One place handles errors for the whole backend.
 *
 * ProblemDetail is a standard JSON error format (RFC 9457) built into Spring:
 *   { "status": 503, "title": "Service Unavailable", "detail": "..." }
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Orthocal is down, slow, or replied with an error.
     * 503 "Service Unavailable" = "not your fault; try again later".
     */
    @ExceptionHandler(RestClientException.class)
    public ProblemDetail calendarUnavailable(RestClientException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The liturgical calendar service is not responding. Please try again in a moment.");
    }

    /**
     * An impossible date was requested, e.g. /api/day/2026/2/30.
     * 400 "Bad Request" = "the request itself was wrong".
     */
    @ExceptionHandler(DateTimeException.class)
    public ProblemDetail invalidDate(DateTimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "That date doesn't exist.");
    }

    /**
     * A sign-in or account problem, e.g. a wrong password (401) or a weak one
     * (400). The message was written to be shown to the user as it is. For weak
     * passwords, "problems" lists every broken rule:
     *   { "status": 400, "detail": "Please choose a stronger password.",
     *     "problems": ["Add a number (0-9)."] }
     */
    @ExceptionHandler(AuthException.class)
    public ProblemDetail authProblem(AuthException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        if (!ex.problems().isEmpty()) {
            problem.setProperty("problems", ex.problems());
        }
        return problem;
    }
}
