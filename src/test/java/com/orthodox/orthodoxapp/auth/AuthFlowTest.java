package com.orthodox.orthodoxapp.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orthodox.orthodoxapp.account.AppUser;
import com.orthodox.orthodoxapp.notify.EmailSender;
import com.orthodox.orthodoxapp.notify.SmsSender;
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
                .andExpect(jsonPath("$.sentTo").value("t•••@example.com"));
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
        AppUser planted = authService.register("victim@example.com", STRONG_PASSWORD, "Not Me", null);

        // ...then the real owner signs in with Google, which has verified the email.
        AppUser owner = authService.signInWithProvider("google", "google-123", "Victim@Example.com", true, "Real Owner");

        assertEquals(planted.getId(), owner.getId());
        assertFalse(owner.hasPassword(), "the unverified password must be removed");
        assertTrue(owner.isEmailVerified());
        assertEquals("google-123", owner.getGoogleId());

        // Next time, Google's ID alone finds the same account.
        assertEquals(owner.getId(),
                authService.signInWithProvider("google", "google-123", "victim@example.com", true, null).getId());
    }

    @Test
    void anUnverifiedEmailFromAProviderIsRefused() {
        assertThrows(AuthException.class,
                () -> authService.signInWithProvider("apple", "apple-1", "x@example.com", false, null));
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

    private static String sixDigits(List<String> messages) {
        Matcher matcher = Pattern.compile("\\d{6}").matcher(messages.get(messages.size() - 1));
        assertTrue(matcher.find(), "the message should contain a 6-digit code");
        return matcher.group();
    }
}
