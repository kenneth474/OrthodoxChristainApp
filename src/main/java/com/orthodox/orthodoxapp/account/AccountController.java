package com.orthodox.orthodoxapp.account;

import com.orthodox.orthodoxapp.auth.SignedInUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AccountController: endpoints for the signed-in person's own account.
 * SecurityConfig makes every address here require a signed-in user.
 *
 *   GET  /api/account                  -> your details (AccountView)
 *   PUT  /api/account/name             -> change your name
 *   PUT  /api/account/phone            -> add, change or remove your phone (texts a code)
 *   POST /api/account/email/send-code  -> email a code to verify your email
 *   POST /api/account/email/verify     -> check that code
 *   POST /api/account/phone/send-code  -> text a code to verify your phone
 *   POST /api/account/phone/verify     -> check that code
 *   PUT  /api/account/two-factor       -> NONE, EMAIL or SMS
 *
 * @AuthenticationPrincipal gives us the SignedInUser from the session, so a
 * person can only ever change their OWN account.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    public record NameRequest(String displayName) {
    }

    public record PhoneRequest(String phone) {
    }

    public record CodeRequest(String code) {
    }

    public record TwoFactorRequest(TwoFactorMethod method) {
    }

    /** Where a code was sent, partly hidden; null when no code was sent. */
    public record CodeSent(String sentTo) {
    }

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public AccountView me(@AuthenticationPrincipal SignedInUser me) {
        return AccountView.from(accountService.get(me.id()));
    }

    @PutMapping("/name")
    public AccountView updateName(@AuthenticationPrincipal SignedInUser me, @RequestBody NameRequest body) {
        return AccountView.from(accountService.updateName(me.id(), body.displayName()));
    }

    @PutMapping("/phone")
    public CodeSent updatePhone(@AuthenticationPrincipal SignedInUser me, @RequestBody PhoneRequest body) {
        return new CodeSent(accountService.updatePhone(me.id(), body.phone()));
    }

    @PostMapping("/email/send-code")
    public CodeSent sendEmailCode(@AuthenticationPrincipal SignedInUser me) {
        return new CodeSent(accountService.sendEmailCode(me.id()));
    }

    @PostMapping("/email/verify")
    public AccountView verifyEmail(@AuthenticationPrincipal SignedInUser me, @RequestBody CodeRequest body) {
        return AccountView.from(accountService.verifyEmail(me.id(), body.code()));
    }

    @PostMapping("/phone/send-code")
    public CodeSent sendPhoneCode(@AuthenticationPrincipal SignedInUser me) {
        return new CodeSent(accountService.sendPhoneCode(me.id()));
    }

    @PostMapping("/phone/verify")
    public AccountView verifyPhone(@AuthenticationPrincipal SignedInUser me, @RequestBody CodeRequest body) {
        return AccountView.from(accountService.verifyPhone(me.id(), body.code()));
    }

    @PutMapping("/two-factor")
    public AccountView setTwoFactor(@AuthenticationPrincipal SignedInUser me, @RequestBody TwoFactorRequest body) {
        return AccountView.from(accountService.setTwoFactor(me.id(), body.method()));
    }
}
