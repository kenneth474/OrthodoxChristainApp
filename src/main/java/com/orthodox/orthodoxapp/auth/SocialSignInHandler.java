package com.orthodox.orthodoxapp.auth;

import com.orthodox.orthodoxapp.account.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * SocialSignInHandler: runs after Google or Apple has confirmed who you are.
 *
 * Spring Security has already checked the provider's ID token. Here we find
 * or create your AppUser (AuthService.signInWithProvider), sign you in with
 * the same kind of session a password sign-in uses, and send you home.
 */
@Component
public class SocialSignInHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final SessionSignIn sessionSignIn;

    public SocialSignInHandler(AuthService authService, SessionSignIn sessionSignIn) {
        this.authService = authService;
        this.sessionSignIn = sessionSignIn;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OidcUser person = (OidcUser) token.getPrincipal();

        // Google sends email_verified as true/false; Apple sends the text "true"/"false".
        // String.valueOf handles both.
        boolean emailVerified = "true".equals(String.valueOf(person.getClaims().get(StandardClaimNames.EMAIL_VERIFIED)));

        try {
            AppUser user = authService.signInWithProvider(
                    token.getAuthorizedClientRegistrationId(), // "google" or "apple"
                    person.getSubject(),
                    person.getEmail(),
                    emailVerified,
                    person.getFullName());
            sessionSignIn.completeSignIn(user, request, response);
            response.sendRedirect("/");
        } catch (AuthException ex) {
            // e.g. no verified email: undo the half-finished sign-in.
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect("/login.html?error=social");
        }
    }
}
