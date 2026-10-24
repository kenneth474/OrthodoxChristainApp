package com.orthodox.orthodoxapp.notify;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * TwilioProperties: the Twilio Verify settings, from secrets.properties on
 * your computer or environment variables online.
 *
 * @ConfigurationProperties("app.sms.twilio") fills this record from lines like
 *   app.sms.twilio.account-sid=AC123...          (online: APP_SMS_TWILIO_ACCOUNTSID)
 *   app.sms.twilio.auth-token=...                (online: APP_SMS_TWILIO_AUTHTOKEN)
 *   app.sms.twilio.verify-service-sid=VA123...   (online: APP_SMS_TWILIO_VERIFYSERVICESID)
 * Spring matches "account-sid" to the field accountSid automatically.
 * Anything missing stays null.
 */
@ConfigurationProperties("app.sms.twilio")
public record TwilioProperties(String accountSid, String authToken, String verifyServiceSid) {

    /** True only when all three settings are filled in. */
    public boolean configured() {
        return StringUtils.hasText(accountSid) && StringUtils.hasText(authToken)
                && StringUtils.hasText(verifyServiceSid);
    }
}
