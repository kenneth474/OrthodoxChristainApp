package com.orthodox.orthodoxapp.auth;

import com.orthodox.orthodoxapp.account.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * SessionSignIn: marks a browser session as signed in (or half signed in).
 *
 * HOW SIGNING IN WORKS
 * The web is "stateless": each request arrives on its own. To remember who
 * you are, the server gives your browser a session cookie (JSESSIONID), a
 * random ID that points to a small storage area on the server: your session.
 * Signing in means writing "this session belongs to account 7" into it.
 * Spring Security then reads that on every later request.
 *
 * With two-step verification there's an in-between state: the password was
 * right but the code hasn't been entered yet. We note that separately
 * ("pending"), so the person is NOT signed in until the code is checked.
 */
@Component
public class SessionSignIn {

    // The pending state expires, like the code itself.
    private static final Duration PENDING_LIFETIME = Duration.ofMinutes(10);
    private static final String PENDING_ATTRIBUTE = "PENDING_SIGN_IN";

    private record PendingSignIn(Long userId, Instant startedAt) implements Serializable {
    }

    // Where Spring Security keeps the signed-in user: inside the HTTP session.
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    /** Fully signs this person in for this browser session. */
    public void completeSignIn(AppUser user, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        session.removeAttribute(PENDING_ATTRIBUTE);

        // Give the session a brand-new ID. This blocks "session fixation": an
        // attacker who planted a session ID in your browser before you signed
        // in can't use it afterwards.
        request.changeSessionId();

        // An "Authentication" is Spring Security's record of who is signed in
        // and what they're allowed to do (ROLE_USER = an ordinary member).
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                new SignedInUser(user.getId(), user.getEmail()),
                null, // no need to keep the password around
                AuthorityUtils.createAuthorityList("ROLE_USER"));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);              // for the rest of this request
        contextRepository.saveContext(context, request, response); // for later requests
    }

    /** Password was right; now waiting for the 6-digit code. */
    public void startTwoStep(AppUser user, HttpServletRequest request) {
        request.getSession(true).setAttribute(PENDING_ATTRIBUTE, new PendingSignIn(user.getId(), Instant.now()));
    }

    /** The account waiting for its code, if it's been less than 10 minutes. */
    public Optional<Long> pendingUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(PENDING_ATTRIBUTE) instanceof PendingSignIn pending)) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(pending.startedAt().plus(PENDING_LIFETIME))) {
            session.removeAttribute(PENDING_ATTRIBUTE);
            return Optional.empty();
        }
        return Optional.of(pending.userId());
    }
}
