package com.orthodox.orthodoxapp.notify;

import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * TwilioSmsSender: sends real text messages through Twilio (twilio.com), a
 * paid service that delivers texts for apps.
 *
 * It calls Twilio's web API the same way OrthocalClient calls Orthocal: one
 * HTTP request per message. Twilio wants a "form" (like a web page form)
 * rather than JSON, and our account ID and token as the login.
 */
public class TwilioSmsSender implements SmsSender {

    private final RestClient restClient;
    private final String fromNumber;

    public TwilioSmsSender(TwilioProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.twilio.com/2010-04-01/Accounts/" + properties.accountSid())
                // Every request logs in to Twilio with "Basic" authentication.
                .defaultHeaders(headers -> headers.setBasicAuth(properties.accountSid(), properties.authToken()))
                .requestFactory(requestFactory)
                .build();
        this.fromNumber = properties.fromNumber();
    }

    @Override
    public void send(String toPhone, String body) {
        // A MultiValueMap is sent as form fields: To=...&From=...&Body=...
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", toPhone);
        form.add("From", fromNumber);
        form.add("Body", body);

        restClient.post()
                .uri("/Messages.json")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity(); // we only care that it worked; errors throw an exception
    }
}
