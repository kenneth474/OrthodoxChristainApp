package com.orthodox.orthodoxapp.auth;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.orthodox.orthodoxapp.account.AppUser;
import com.orthodox.orthodoxapp.notify.EmailSender;
import com.orthodox.orthodoxapp.notify.SmsSender;
import com.orthodox.orthodoxapp.notify.TwilioVerify;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * AuthFlowTest: tests the whole sign-in system, end to end.
 *
 * @SpringBootTest starts the real app (all our classes, Spring Security, the
 * database) but with an in-memory database that disappears afterwards.
 * MockMvc sends pretend web requests, exactly as a browser would, without a
 * real server. MockHttpSession plays the part of the browser's session cookie.
 *
 * @MockitoBean swaps the email and text senders for "mocks": fakes that send
 * nothing but remember what they were asked to send, so the test can read
 * the 6-digit codes.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:authflow;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AuthFlowTest {

    private static final String STRONG_PASSWORD = "Vespers-at-6:30pm";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AuthService authService;

    @MockitoBean
    private EmailSender emailSender;

    @MockitoBean
    private SmsSender smsSender;

    // A pretend Twilio Verify: switched off (configured() = false) unless a test says otherwise.
    @MockitoBean
    private TwilioVerify twilioVerify;

    // ----- Creating an account -----

    @Test
    void aWeakPasswordIsRejectedWithEveryProblemListed() throws Exception {
        send(post("/api/auth/register"), new MockHttpSession(), """
                {"email": "weak@example.com", "password": "short", "displayName": "Weak"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Please choose a stronger password."))
                .andExpect(jsonPath("$.problems.length()").value(4));
    }

    @Test
    void registeringSignsYouInAndSigningOutSignsYouOut() throws Exception {
        MockHttpSession browser = register("anna@example.com", "(214) 555-0123");

        mvc.perform(get("/api/account").session(browser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("anna@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("+12145550123"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.twoFactorMethod").value("NONE"));

        mvc.perform(post("/api/auth/sign-out").session(browser).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/account").session(new MockHttpSession()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theSameEmailCantRegisterTwice() throws Exception {
        register("twice@example.com", null);
        send(post("/api/auth/register"), new MockHttpSession(), registration("TWICE@example.com", null))
                .andExpect(status().isConflict());
    }

    // ----- Signing in -----

    @Test
    void wrongPasswordsAreRefusedAndFiveInARowLockTheAccount() throws Exception {
        register("locked@example.com", null);
        String wrong = """
                {"email": "locked@example.com", "password": "Not-the-password-1"}""";

        for (int attempt = 1; attempt <= 5; attempt++) {
            send(post("/api/auth/sign-in"), new MockHttpSession(), wrong)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Email or password is incorrect."));
        }
        // Now even the right password is refused for 15 minutes.
        send(post("/api/auth/sign-in"), new MockHttpSession(), signIn("locked@example.com"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void anUnknownEmailGetsTheSameMessageAsAWrongPassword() throws Exception {
        send(post("/api/auth/sign-in"), new MockHttpSession(), signIn("nobody@example.com"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email or password is incorrect."));
    }

    // ----- Two-step verification -----

    @Test
    void emailTwoStepVerificationAsksForACodeAfterThePassword() throws Exception {
        String email = "twostep@example.com";
        MockHttpSession browser = register(email, null);

        // Verify the email address with the code we "emailed".
        send(post("/api/account/email/send-code"), browser, null).andExpect(status().isOk());
        send(post("/api/account/email/verify"), browser, code(lastCodeEmailedTo(email)))
                .andExpect(jsonPath("$.emailVerified").value(true));

        // Turn on two-step verification by email, then sign out.
        send(put("/api/account/two-factor"), browser, """
                {"method": "EMAIL"}""")
                .andExpect(jsonPath("$.twoFactorMethod").value("EMAIL"));
        mvc.perform(post("/api/auth/sign-out").session(browser).with(csrf()));

        // Sign in again: the right password alone is NOT enough.
        MockHttpSession newBrowser = new MockHttpSession();
        send(post("/api/auth/sign-in"), newBrowser, signIn(email))
                .andExpect(jsonPath("$.status").value("CODE_REQUIRED"))
                .andExpect(jsonPath("$.sentTo").value("t•••@example.com"))
                .andExpect(jsonPath("$.developmentMode").value(false)); // the test's fake sender is "real"
        mvc.perform(get("/api/account").session(newBrowser)).andExpect(status().isUnauthorized());

        String rightCode = lastCodeEmailedTo(email);
        String wrongCode = rightCode.equals("000000") ? "111111" : "000000";
        send(post("/api/auth/sign-in/verify"), newBrowser, code(wrongCode))
                .andExpect(status().isBadRequest());

        send(post("/api/auth/sign-in/verify"), newBrowser, code(rightCode))
                .andExpect(jsonPath("$.status").value("SIGNED_IN"));
        mvc.perform(get("/api/account").session(newBrowser)).andExpect(status().isOk());
    }

    @Test
    void twoStepByTextNeedsAVerifiedPhone() throws Exception {
        MockHttpSession browser = register("texter@example.com", null);

        send(put("/api/account/two-factor"), browser, """
                {"method": "SMS"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Add and verify your phone number first."));

        // Adding a phone number texts it a code straight away.
        send(put("/api/account/phone"), browser, """
                {"phone": "214-555-0199"}""")
                .andExpect(jsonPath("$.sentTo").value("•••• 0199"));
        send(post("/api/account/phone/verify"), browser, code(lastCodeTextedTo("+12145550199")))
                .andExpect(jsonPath("$.phoneVerified").value(true));

        send(put("/api/account/two-factor"), browser, """
                {"method": "SMS"}""")
                .andExpect(jsonPath("$.twoFactorMethod").value("SMS"));
    }

    @Test
    void textCodesGoThroughTwilioVerifyWhenItIsSetUp() throws Exception {
        when(twilioVerify.configured()).thenReturn(true);
        when(twilioVerify.check("+12145550188", "246810")).thenReturn(true);
        MockHttpSession browser = register("verify@example.com", null);

        // Adding a phone asks Twilio to text a code (Twilio makes it, not us).
        send(put("/api/account/phone"), browser, """
                {"phone": "214-555-0188"}""")
                .andExpect(jsonPath("$.sentTo").value("•••• 0188"))
                .andExpect(jsonPath("$.developmentMode").value(false));
        verify(twilioVerify).start("+12145550188");

        // Twilio says a wrong code is wrong, and the right one is right.
        send(post("/api/account/phone/verify"), browser, code("135791"))
                .andExpect(status().isBadRequest());
        send(post("/api/account/phone/verify"), browser, code("246810"))
                .andExpect(jsonPath("$.phoneVerified").value(true));

        send(put("/api/account/two-factor"), browser, """
                {"method": "SMS"}""")
                .andExpect(jsonPath("$.twoFactorMethod").value("SMS"));
    }

    @Test
    void theOptionsSayWhatThisServerCanDo() throws Exception {
        // Tests run without Google keys; codes are "sent" by the pretend senders.
        mvc.perform(get("/api/auth/options"))
                .andExpect(jsonPath("$.google").value(false))
                .andExpect(jsonPath("$.emailCodes").value(true))
                .andExpect(jsonPath("$.textCodes").value(true));
    }

    // ----- Authenticator app -----

    @Test
    void anAuthenticatorAppCanBeSetUpAndAsksForItsCodeAfterThePassword() throws Exception {
        String email = "app@example.com";
        MockHttpSession browser = register(email, null);

        // Choosing it directly isn't allowed: the app must prove it has the secret first.
        send(put("/api/account/two-factor"), browser, """
                {"method": "AUTHENTICATOR"}""")
                .andExpect(status().isConflict());

        String secret = startAuthenticatorSetup(browser);
        send(post("/api/account/authenticator/confirm"), browser, code(wrongCode(secret)))
                .andExpect(status().isBadRequest());
        long step = Totp.stepAt(Instant.now());
        send(post("/api/account/authenticator/confirm"), browser, code(Totp.code(secret, step)))
                .andExpect(jsonPath("$.account.twoFactorMethod").value("AUTHENTICATOR"))
                .andExpect(jsonPath("$.backupCodes.length()").value(10));
        mvc.perform(post("/api/auth/sign-out").session(browser).with(csrf()));

        // Signing in: the password, then the app's code. Nothing is sent.
        MockHttpSession newBrowser = new MockHttpSession();
        send(post("/api/auth/sign-in"), newBrowser, signIn(email))
                .andExpect(jsonPath("$.status").value("CODE_REQUIRED"))
                .andExpect(jsonPath("$.method").value("AUTHENTICATOR"))
                .andExpect(jsonPath("$.sentTo").doesNotExist());
        send(post("/api/auth/sign-in/resend"), newBrowser, null).andExpect(status().isConflict());

        // The code already used during setup can't be used again...
        send(post("/api/auth/sign-in/verify"), newBrowser, code(Totp.code(secret, step)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("That code was just used. Wait for your app to show the next one."));
        // ...but the next one works.
        send(post("/api/auth/sign-in/verify"), newBrowser, code(Totp.code(secret, step + 1)))
                .andExpect(jsonPath("$.status").value("SIGNED_IN"));
        mvc.perform(get("/api/account").session(newBrowser)).andExpect(status().isOk());
    }

    @Test
    void eachBackupCodeWorksOnce() throws Exception {
        String email = "backup@example.com";
        MockHttpSession browser = register(email, null);
        List<String> backupCodes = turnOnAuthenticator(browser);
        mvc.perform(post("/api/auth/sign-out").session(browser).with(csrf()));

        MockHttpSession first = new MockHttpSession();
        send(post("/api/auth/sign-in"), first, signIn(email));
        send(post("/api/auth/sign-in/verify"), first, code(backupCodes.get(0).toLowerCase()))
                .andExpect(jsonPath("$.status").value("SIGNED_IN"));

        MockHttpSession second = new MockHttpSession();
        send(post("/api/auth/sign-in"), second, signIn(email));
        send(post("/api/auth/sign-in/verify"), second, code(backupCodes.get(0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tenWrongCodesLockTheAccountEvenIfThePasswordIsTypedAgain() throws Exception {
        String email = "guesser@example.com";
        MockHttpSession browser = register(email, null);
        String secret = startAuthenticatorSetup(browser);
        send(post("/api/account/authenticator/confirm"), browser, code(Totp.code(secret, Totp.stepAt(Instant.now()))));
        mvc.perform(post("/api/auth/sign-out").session(browser).with(csrf()));

        // Five wrong codes, then the right password again (which resets the
        // PASSWORD count), then five more wrong codes: the account locks.
        for (int round = 0; round < 2; round++) {
            MockHttpSession attacker = new MockHttpSession();
            send(post("/api/auth/sign-in"), attacker, signIn(email))
                    .andExpect(jsonPath("$.status").value("CODE_REQUIRED"));
            for (int guess = 0; guess < 5; guess++) {
                send(post("/api/auth/sign-in/verify"), attacker, code(wrongCode(secret)))
                        .andExpect(status().isBadRequest());
            }
        }
        send(post("/api/auth/sign-in"), new MockHttpSession(), signIn(email))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void turningTheAuthenticatorOffForgetsIt() throws Exception {
        MockHttpSession browser = register("off@example.com", null);
        turnOnAuthenticator(browser);

        send(put("/api/account/two-factor"), browser, """
                {"method": "NONE"}""")
                .andExpect(jsonPath("$.twoFactorMethod").value("NONE"));
        // Its secret is gone, so it can't simply be switched back on.
        send(put("/api/account/two-factor"), browser, """
                {"method": "AUTHENTICATOR"}""")
                .andExpect(status().isConflict());
        send(post("/api/account/authenticator/backup-codes"), browser, null)
                .andExpect(status().isConflict());
    }

    // ----- Security basics -----

    @Test
    void accountDetailsNeedASignedInUser() throws Exception {
        mvc.perform(get("/api/account")).andExpect(status().isUnauthorized());
    }

    @Test
    void requestsWithoutACsrfTokenAreRefused() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration("csrf@example.com", null)))
                .andExpect(status().isForbidden());
    }

    // ----- Google / Apple -----

    @Test
    void googleSignInTakesOverAnAccountWhoseEmailWasNeverVerified() {
        // Someone registers with an email they don't own and never verifies it...
        AppUser planted = authService.register("victim@example.com", STRONG_PASSWORD, "Not Me", null, null);

        // ...then the real owner signs in with Google, which has verified the email.
        AppUser owner = authService.signInWithGoogle("google-123", "Victim@Example.com", true, "Real Owner");

        assertEquals(planted.getId(), owner.getId());
        assertFalse(owner.hasPassword(), "the unverified password must be removed");
        assertTrue(owner.isEmailVerified());
        assertEquals("google-123", owner.getGoogleId());

        // Next time, Google's ID alone finds the same account.
        assertEquals(owner.getId(),
                authService.signInWithGoogle("google-123", "victim@example.com", true, null).getId());
    }

    @Test
    void anUnverifiedEmailFromGoogleIsRefused() {
        assertThrows(AuthException.class,
                () -> authService.signInWithGoogle("google-1", "x@example.com", false, null));
    }

    // ----- Helpers -----

    // Sends a JSON request in a "browser" session, with a valid CSRF token.
    private ResultActions send(MockHttpServletRequestBuilder request, MockHttpSession browser, String json)
            throws Exception {
        request.session(browser).with(csrf());
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(request);
    }

    private MockHttpSession register(String email, String phone) throws Exception {
        MockHttpSession browser = new MockHttpSession();
        send(post("/api/auth/register"), browser, registration(email, phone))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SIGNED_IN"));
        return browser;
    }

    private static String registration(String email, String phone) {
        return """
                {"email": "%s", "password": "%s", "displayName": "Test Person", "phone": %s}"""
                .formatted(email, STRONG_PASSWORD, phone == null ? "null" : "\"" + phone + "\"");
    }

    private static String signIn(String email) {
        return """
                {"email": "%s", "password": "%s"}""".formatted(email, STRONG_PASSWORD);
    }

    private static String code(String code) {
        return """
                {"code": "%s"}""".formatted(code);
    }

    // An ArgumentCaptor collects what the mock was given; we take the newest message.
    private String lastCodeEmailedTo(String email) {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeastOnce()).send(eq(email), anyString(), body.capture());
        return sixDigits(body.getAllValues());
    }

    private String lastCodeTextedTo(String phone) {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(smsSender, atLeastOnce()).send(eq(phone), body.capture());
        return sixDigits(body.getAllValues());
    }

    // Starts setting up an authenticator app and returns its secret (from the QR code).
    private String startAuthenticatorSetup(MockHttpSession browser) throws Exception {
        String json = send(post("/api/account/authenticator/setup"), browser, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uri").value(startsWith("otpauth://totp/")))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.secret");
    }

    // Sets up an authenticator app and returns the backup codes.
    private List<String> turnOnAuthenticator(MockHttpSession browser) throws Exception {
        String secret = startAuthenticatorSetup(browser);
        String json = send(post("/api/account/authenticator/confirm"), browser,
                code(Totp.code(secret, Totp.stepAt(Instant.now()))))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.backupCodes");
    }

    // A 6-digit code that isn't right just now (nor half a minute either side).
    private static String wrongCode(String secret) {
        long step = Totp.stepAt(Instant.now());
        for (int n = 0; ; n++) {
            String candidate = "%06d".formatted(n);
            if (Totp.verify(secret, candidate, Instant.now()).isEmpty()
                    && !candidate.equals(Totp.code(secret, step + 2))) {
                return candidate;
            }
        }
    }

    private static String sixDigits(List<String> messages) {
        Matcher matcher = Pattern.compile("\\d{6}").matcher(messages.get(messages.size() - 1));
        assertTrue(matcher.find(), "the message should contain a 6-digit code");
        return matcher.group();
    }
}
