package com.orthodox.orthodoxapp.notify;

import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * TwilioVerify: sign-in and phone codes by text message, through Twilio
 * Verify (twilio.com/verify).
 *
 * WHY VERIFY, AND NOT ORDINARY TEXTS?
 * US phone carriers now block texts from apps that haven't registered with
 * them ("A2P 10DLC": fees and days of paperwork). Texts that only carry
 * verification codes can go through Twilio Verify instead, which uses
 * Twilio's own registered numbers, so there's nothing to register or buy.
 * It costs about $0.05 per successful verification plus about a cent per
 * text (US prices on twilio.com/en-us/verify/pricing).
 *
 * HOW IT WORKS
 * Twilio makes the 6-digit code itself and texts it ("Your Orthodox Daily
 * verification code is: 123456"). When the person types it, we ask Twilio
 * whether it's right. So for texts, our database never sees the code at all.
 *
 * It's switched on by three settings (secrets.properties, or Render's
 * Environment): APP_SMS_TWILIO_ACCOUNTSID, APP_SMS_TWILIO_AUTHTOKEN and
 * APP_SMS_TWILIO_VERIFYSERVICESID. Without them, configured() is false and
 * texts are printed in the terminal while developing (ConsoleSmsSender).
 */
@Component
public class TwilioVerify {

    private final RestClient restClient; // null when not set up

    @Autowired
    public TwilioVerify(TwilioProperties properties) {
        this(properties, RestClient.builder().requestFactory(withTimeouts()));
    }

    /** For tests, which pass in a builder connected to a pretend Twilio. */
    TwilioVerify(TwilioProperties properties, RestClient.Builder builder) {
        this.restClient = !properties.configured() ? null : builder
                .baseUrl("https://verify.twilio.com/v2/Services/" + properties.verifyServiceSid())
                // Every request logs in to Twilio with "Basic" authentication.
                .defaultHeaders(headers -> headers.setBasicAuth(properties.accountSid(), properties.authToken()))
                .build();
    }

    // Give up after 5 seconds connecting or 10 seconds waiting, rather than hanging.
    private static SimpleClientHttpRequestFactory withTimeouts() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return requestFactory;
    }

    public boolean configured() {
        return restClient != null;
    }

    /** Asks Twilio to text a new code to a phone (E.164, e.g. +12145550123). Errors throw an exception. */
    public void start(String phone) {
        // A MultiValueMap is sent as form fields: To=...&Channel=sms
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", phone);
        form.add("Channel", "sms");
        restClient.post()
                .uri("/Verifications")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Asks Twilio whether a typed code is right for that phone.
     *
     * @return true if Twilio says "approved"
     */
    public boolean check(String phone, String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", phone);
        form.add("Code", code);
        try {
            Map<?, ?> result = restClient.post()
                    .uri("/VerificationCheck")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            return result != null && "approved".equals(result.get("status"));
        } catch (HttpClientErrorException e) {
            // 404: Twilio has no waiting code for this phone (it expired, or
            // was guessed wrong too often). That's simply "not right".
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw e;
        }
    }
}
