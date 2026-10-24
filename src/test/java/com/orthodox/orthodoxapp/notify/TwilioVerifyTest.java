package com.orthodox.orthodoxapp.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Checks what TwilioVerify sends to Twilio, using MockRestServiceServer: a
 * pretend Twilio that checks each request and gives a prepared answer.
 */
class TwilioVerifyTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer twilio = MockRestServiceServer.bindTo(builder).build();
    private final TwilioVerify verify =
            new TwilioVerify(new TwilioProperties("AC123", "secret-token", "VA456"), builder);

    private static final String LOGIN = "Basic "
            + Base64.getEncoder().encodeToString("AC123:secret-token".getBytes(StandardCharsets.UTF_8));

    @Test
    void startAsksTwilioToTextACode() {
        twilio.expect(requestTo("https://verify.twilio.com/v2/Services/VA456/Verifications"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", LOGIN))
                .andExpect(content().formDataContains(java.util.Map.of("To", "+12145550123", "Channel", "sms")))
                .andRespond(withSuccess("{\"status\": \"pending\"}", MediaType.APPLICATION_JSON));

        verify.start("+12145550123");
        twilio.verify();
    }

    @Test
    void checkIsTrueOnlyWhenTwilioApproves() {
        twilio.expect(requestTo("https://verify.twilio.com/v2/Services/VA456/VerificationCheck"))
                .andExpect(content().formDataContains(java.util.Map.of("To", "+12145550123", "Code", "123456")))
                .andRespond(withSuccess("{\"status\": \"approved\"}", MediaType.APPLICATION_JSON));
        twilio.expect(requestTo("https://verify.twilio.com/v2/Services/VA456/VerificationCheck"))
                .andRespond(withSuccess("{\"status\": \"pending\"}", MediaType.APPLICATION_JSON));
        twilio.expect(requestTo("https://verify.twilio.com/v2/Services/VA456/VerificationCheck"))
                .andRespond(withResourceNotFound()); // no waiting code: expired or used up

        assertThat(verify.check("+12145550123", "123456")).isTrue();
        assertThat(verify.check("+12145550123", "000000")).isFalse();
        assertThat(verify.check("+12145550123", "000000")).isFalse();
        twilio.verify();
    }

    @Test
    void withoutAllThreeSettingsItIsSwitchedOff() {
        assertThat(new TwilioVerify(new TwilioProperties("AC123", "secret-token", null)).configured()).isFalse();
        assertThat(verify.configured()).isTrue();
    }
}
