package com.orthodox.orthodoxapp.auth;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * AuthException: a sign-in or account action can't be done, with a message
 * that's safe to show the user.
 *
 * ApiExceptionHandler turns it into a JSON error with this status, e.g.
 *   { "status": 401, "detail": "Email or password is incorrect." }
 * For a weak password, "problems" also lists every rule the password broke.
 */
public class AuthException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> problems;

    public AuthException(HttpStatus status, String message) {
        this(status, message, List.of());
    }

    public AuthException(HttpStatus status, String message, List<String> problems) {
        super(message);
        this.status = status;
        this.problems = List.copyOf(problems);
    }

    public HttpStatus status() {
        return status;
    }

    public List<String> problems() {
        return problems;
    }
}
