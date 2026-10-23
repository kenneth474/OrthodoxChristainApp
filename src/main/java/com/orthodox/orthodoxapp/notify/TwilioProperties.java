package com.orthodox.orthodoxapp.notify;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * TwilioProperties: the Twilio settings from secrets.properties.
 *
 * @ConfigurationProperties("app.sms.twilio") fills this record from lines like
 *   app.sms.twilio.account-sid=AC123...
 *   app.sms.twilio.auth-token=...
 *   app.sms.twilio.from-number=+12145550000
 * Spring matches "account-sid" to the field accountSid automatically.
 * Anything missing stays null.
 */
@ConfigurationProperties("app.sms.twilio")
public record TwilioProperties(String accountSid, String authToken, String fromNumber) {

    /** True only when all three settings are filled in. */
    public boolean configured() {
        return StringUtils.hasText(accountSid) && StringUtils.hasText(authToken) && StringUtils.hasText(fromNumber);
    }
}
