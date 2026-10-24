package com.orthodox.orthodoxapp.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The app as it runs online before email and texts are set up: the real
 * development senders (which only print codes), with printing switched off
 * as in application-prod.properties. Email and text codes must then not be
 * offered at all, so nobody can lock themselves out.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:online;DB_CLOSE_DELAY=-1",
        "app.notify.development-codes=false"})
@AutoConfigureMockMvc
class OnlineCodeChannelsTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void emailAndTextCodesAreNotOffered() throws Exception {
        mvc.perform(get("/api/auth/options"))
                .andExpect(jsonPath("$.emailCodes").value(false))
                .andExpect(jsonPath("$.textCodes").value(false));

        MockHttpSession browser = new MockHttpSession();
        mvc.perform(post("/api/auth/register").session(browser).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "online@example.com", "password": "Vespers-at-6:30pm", "displayName": "Test"}"""))
                .andExpect(status().isCreated());

        // No email code to verify with, and no email two-step verification.
        mvc.perform(post("/api/account/email/send-code").session(browser).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/account/two-factor").session(browser).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method": "EMAIL"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Email codes aren't available on this server yet. Use an authenticator app instead."));

        // A phone number is still saved, just not texted a code.
        mvc.perform(put("/api/account/phone").session(browser).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": "214-555-0177"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentTo").doesNotExist());
        mvc.perform(get("/api/account").session(browser))
                .andExpect(jsonPath("$.phoneNumber").value("+12145550177"))
                .andExpect(jsonPath("$.phoneVerified").value(false));

        // The authenticator app needs neither, so it's always offered.
        mvc.perform(post("/api/account/authenticator/setup").session(browser).with(csrf()))
                .andExpect(status().isOk());
    }
}
