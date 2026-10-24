package com.orthodox.orthodoxapp.account;

import com.orthodox.orthodoxapp.auth.AuthException;
import com.orthodox.orthodoxapp.auth.AuthenticatorService;
import com.orthodox.orthodoxapp.auth.SentCode;
import com.orthodox.orthodoxapp.auth.SignedInUser;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * AccountController: endpoints for the signed-in person's own account.
 * SecurityConfig makes every address here require a signed-in user.
 *
 *   GET    /api/account                  -> your details (AccountView)
 *   PUT    /api/account/profile          -> name, patron saint, name day, home parish
 *   PUT    /api/account/appearance       -> text size, light/dark theme and style
 *   GET    /api/account/photo            -> your photo (a JPEG)
 *   POST   /api/account/photo            -> upload a new photo
 *   DELETE /api/account/photo            -> remove it
 *   PUT    /api/account/phone            -> add, change or remove your phone (texts a code)
 *   POST   /api/account/email/send-code  -> email a code to verify your email
 *   POST   /api/account/email/verify     -> check that code
 *   POST   /api/account/phone/send-code  -> text a code to verify your phone
 *   POST   /api/account/phone/verify     -> check that code
 *   PUT    /api/account/two-factor       -> NONE, EMAIL or SMS (or keep AUTHENTICATOR)
 *   POST   /api/account/authenticator/setup         -> a new secret and QR code contents
 *   POST   /api/account/authenticator/confirm       -> the app's first code: switches it on, gives backup codes
 *   POST   /api/account/authenticator/backup-codes  -> 10 new backup codes (the old ones stop working)
 *
 * @AuthenticationPrincipal gives us the SignedInUser from the session, so a
 * person can only ever see and change their OWN account.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    public record ProfileRequest(String displayName, String patronSaint, String nameDay, String homeParish) {
    }

    public record AppearanceRequest(TextSize textSize, Theme theme, AppStyle style) {
    }

    public record PhoneRequest(String phone) {
    }

    public record CodeRequest(String code) {
    }

    public record TwoFactorRequest(TwoFactorMethod method) {
    }

    /** The account after turning the app on, and its backup codes (shown once). */
    public record AuthenticatorOn(AccountView account, List<String> backupCodes) {
    }

    public record BackupCodes(List<String> backupCodes) {
    }

    private final AccountService accountService;
    private final ProfilePhotoService photoService;
    private final AuthenticatorService authenticator;

    public AccountController(AccountService accountService, ProfilePhotoService photoService,
                             AuthenticatorService authenticator) {
        this.accountService = accountService;
        this.photoService = photoService;
        this.authenticator = authenticator;
    }

    @GetMapping
    public AccountView me(@AuthenticationPrincipal SignedInUser me) {
        return AccountView.from(accountService.get(me.id()));
    }

    @PutMapping("/profile")
    public AccountView updateProfile(@AuthenticationPrincipal SignedInUser me, @RequestBody ProfileRequest body) {
        return AccountView.from(accountService.updateProfile(
                me.id(), body.displayName(), body.patronSaint(), body.nameDay(), body.homeParish()));
    }

    @PutMapping("/appearance")
    public AccountView updateAppearance(@AuthenticationPrincipal SignedInUser me, @RequestBody AppearanceRequest body) {
        return AccountView.from(accountService.updateAppearance(
                me.id(), body.textSize(), body.theme(), body.style()));
    }

    // ----- Photo -----

    /**
     * Sends the photo's bytes back as an image. ResponseEntity lets us set the
     * status and the Content-Type header ourselves; 404 = no photo yet.
     */
    @GetMapping("/photo")
    public ResponseEntity<byte[]> photo(@AuthenticationPrincipal SignedInUser me) {
        return photoService.find(me.id())
                .map(jpeg -> ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(jpeg))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Uploads arrive as "multipart/form-data", the format browsers use for
     * files. @RequestParam("photo") picks out the file field named "photo".
     */
    @PostMapping("/photo")
    public AccountView uploadPhoto(@AuthenticationPrincipal SignedInUser me,
                                   @RequestParam("photo") MultipartFile photo) throws IOException {
        if (photo.isEmpty()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Please choose a photo.");
        }
        return AccountView.from(photoService.save(me.id(), photo.getBytes()));
    }

    @DeleteMapping("/photo")
    public AccountView deletePhoto(@AuthenticationPrincipal SignedInUser me) {
        return AccountView.from(photoService.delete(me.id()));
    }

    // ----- Phone, verification and two-step verification -----

    @PutMapping("/phone")
    public SentCode updatePhone(@AuthenticationPrincipal SignedInUser me, @RequestBody PhoneRequest body) {
        return accountService.updatePhone(me.id(), body.phone());
    }

    @PostMapping("/email/send-code")
    public SentCode sendEmailCode(@AuthenticationPrincipal SignedInUser me) {
        return accountService.sendEmailCode(me.id());
    }

    @PostMapping("/email/verify")
    public AccountView verifyEmail(@AuthenticationPrincipal SignedInUser me, @RequestBody CodeRequest body) {
        return AccountView.from(accountService.verifyEmail(me.id(), body.code()));
    }

    @PostMapping("/phone/send-code")
    public SentCode sendPhoneCode(@AuthenticationPrincipal SignedInUser me) {
        return accountService.sendPhoneCode(me.id());
    }

    @PostMapping("/phone/verify")
    public AccountView verifyPhone(@AuthenticationPrincipal SignedInUser me, @RequestBody CodeRequest body) {
        return AccountView.from(accountService.verifyPhone(me.id(), body.code()));
    }

    @PutMapping("/two-factor")
    public AccountView setTwoFactor(@AuthenticationPrincipal SignedInUser me, @RequestBody TwoFactorRequest body) {
        return AccountView.from(accountService.setTwoFactor(me.id(), body.method()));
    }

    // ----- Authenticator app (see AuthenticatorService) -----

    /** HttpSession: the new secret waits in your session until confirm. */
    @PostMapping("/authenticator/setup")
    public AuthenticatorService.Setup startAuthenticator(@AuthenticationPrincipal SignedInUser me, HttpSession session) {
        return authenticator.startSetup(accountService.get(me.id()), session);
    }

    @PostMapping("/authenticator/confirm")
    public AuthenticatorOn confirmAuthenticator(@AuthenticationPrincipal SignedInUser me, HttpSession session,
                                                @RequestBody CodeRequest body) {
        AppUser user = accountService.get(me.id());
        List<String> codes = authenticator.confirmSetup(user, session, body.code());
        return new AuthenticatorOn(AccountView.from(user), codes);
    }

    @PostMapping("/authenticator/backup-codes")
    public BackupCodes newBackupCodes(@AuthenticationPrincipal SignedInUser me) {
        return new BackupCodes(authenticator.newBackupCodes(accountService.get(me.id())));
    }
}
