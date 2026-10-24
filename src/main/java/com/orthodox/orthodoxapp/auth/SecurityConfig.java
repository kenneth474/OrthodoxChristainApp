package com.orthodox.orthodoxapp.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * SecurityConfig: the security rules for the whole app.
 *
 * Spring Security checks every request BEFORE it reaches a controller, using
 * a chain of "filters" (the SecurityFilterChain built below). It:
 *   - decides which addresses need a signed-in user
 *   - loads the signed-in user from the session
 *   - blocks CSRF attacks (see below)
 *   - handles signing out, and Google/Apple sign-in
 *
 * CSRF ("cross-site request forgery"): a bad website could make your browser
 * secretly send a request to our app, with your session cookie attached.
 * To block that, every POST/PUT must also carry a secret token that only our
 * own pages can read (they get it from /api/auth/csrf; see js/api.js).
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    /**
     * Hashes passwords with BCrypt. BCrypt is deliberately slow (about a tenth
     * of a second per check), which is nothing for one sign-in but makes
     * guessing billions of passwords impractical. Hashes are saved with a
     * "{bcrypt}" label, so we could move to a newer method later.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    SocialClientRegistrations socialClientRegistrations(AuthProperties properties) {
        return new SocialClientRegistrations(properties);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            SocialClientRegistrations social,
                                            SocialSignInHandler socialSignInHandler) throws Exception {
        http
            // WHO CAN SEE WHAT. Rules are checked top to bottom; the first match wins.
            // Anything not listed needs a signed-in user ("deny by default"), so
            // a new page or endpoint is private until we decide otherwise.
            .authorizeHttpRequests(auth -> auth
                // The React app: its page, its files (built by Vite into assets/),
                // the home-screen icons, and the prayer book data. Every screen's
                // address is public too; screens that need an account ask the API,
                // which answers 401 when you're not signed in.
                .requestMatchers("/", "/index.html", "/assets/**", "/icons/**", "/data/**",
                        "/manifest.webmanifest", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/bible", "/bible/**", "/prayers", "/prayers/**", "/church", "/church/**",
                        "/profile", "/sign-in").permitAll()
                .requestMatchers("/api/today", "/api/day/**", "/api/bible/**", "/api/auth/**").permitAll()
                .anyRequest().authenticated())

            // Apple sends people back with a POST from appleid.apple.com, which
            // can't carry our CSRF token. That address is protected another way:
            // Spring checks the "state" value it sent to Apple.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/login/oauth2/code/*"))

            // For /api/... addresses, "not signed in" is a plain 401 error (the
            // page's JavaScript then shows the sign-in page) instead of a redirect.
            .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    PathPatternRequestMatcher.withDefaults().matcher("/api/**")))

            // POST /api/auth/sign-out ends the session and replies 200 OK.
            .logout(logout -> logout
                .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/auth/sign-out"))
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()));

        // Google/Apple sign-in, only if at least one is set up in secrets.properties.
        if (social.anyEnabled()) {
            http.oauth2Login(oauth -> oauth
                .loginPage("/sign-in")   // the React app's sign-in screen
                .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(appleFormPost(social)))
                .successHandler(socialSignInHandler)
                .failureUrl("/sign-in?error=social"));
        }
        return http.build();
    }

    /**
     * Apple only shares a person's name and email if the reply comes back as
     * a form POST ("response_mode=form_post"), so we add that to requests
     * going to Apple. Google requests are left as they are.
     */
    private static OAuth2AuthorizationRequestResolver appleFormPost(ClientRegistrationRepository registrations) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(request -> request.attributes(attributes -> {
            if ("apple".equals(attributes.get(OAuth2ParameterNames.REGISTRATION_ID))) {
                request.additionalParameters(params -> params.put("response_mode", "form_post"));
            }
        }));
        return resolver;
    }
}
