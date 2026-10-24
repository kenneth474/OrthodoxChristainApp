package com.orthodox.orthodoxapp.notify;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * CodeChannels: can this server really send codes by email, and by text?
 *
 * On your computer, codes that can't be sent are printed in the terminal
 * instead ("development mode"), which is handy for testing. Online that
 * would be a trap: someone could turn on email two-step verification, and
 * then never receive a code to sign in with. So application-prod.properties
 * sets app.notify.development-codes=false, and the app only offers email or
 * text codes when they're really sent.
 */
@Component
public class CodeChannels {

    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final TwilioVerify twilioVerify;
    private final boolean developmentCodesAllowed;

    public CodeChannels(EmailSender emailSender, SmsSender smsSender, TwilioVerify twilioVerify,
                        @Value("${app.notify.development-codes:true}") boolean developmentCodesAllowed) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.twilioVerify = twilioVerify;
        this.developmentCodesAllowed = developmentCodesAllowed;
    }

    /** True if codes can go out by email (really, or printed while developing). */
    public boolean emailAvailable() {
        return !emailSender.developmentMode() || developmentCodesAllowed;
    }

    /** True if codes can go out by text message (through Twilio Verify, or printed while developing). */
    public boolean textAvailable() {
        return twilioVerify.configured() || !smsSender.developmentMode() || developmentCodesAllowed;
    }
}
