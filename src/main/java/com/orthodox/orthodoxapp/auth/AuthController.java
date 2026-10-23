package com.orthodox.orthodoxapp.auth;

import com.orthodox.orthodoxapp.account.AccountService;
import com.orthodox.orthodoxapp.account.AccountView;
import com.orthodox.orthodoxapp.account.AppUser;
import com.orthodox.orthodoxapp.account.TwoFactorMethod;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuthController: the web endpoints for creating accounts and signing in.
 *
 *   GET  /api/auth/csrf            -> the CSRF token pages must send with POST/PUT
 *   GET  /api/auth/options         -> which sign-in buttons to show (Google? Apple?)
 *   POST /api/auth/register        -> create an account (then signed in)
 *   POST /api/auth/sign-in         -> email + password
 *   POST /api/auth/sign-in/verify  -> the 6-digit code, if two-step verification is on
 *   POST /api/auth/sign-in/resend  -> send a new code
 *   POST /api/auth/sign-out        -> handled by Spring Security (see SecurityConfig)
 *
 * @RequestBody turns the JSON the page sends into one of the small records
 * below, e.g. {"email": "...", "password": "..."} -> SignInRequest.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record RegisterRequest(String email, String password, String displayName, String phone) {
    }

    public record SignInRequest(String email, String password) {
    }

    public record CodeRequest(String code) {
    }

    /** Which buttons the sign-in page should show. */
    public record SignInOptions(boolean google, boolean apple) {
    }

    /**
     * The result of a sign-in step:
     *   status "SIGNED_IN"     -> done; account holds the person's details
     *   status "CODE_REQUIRED" -> enter the code sent to sentTo (e.g. "k•••@gmail.com");
     *                             developmentMode = it's in the terminal, not really sent
     */
    public record SignInResponse(String status, String sentTo, boolean developmentMode, AccountView account) {

        static SignInResponse signedIn(AppUser user) {
            return new SignInResponse("SIGNED_IN", null, false, AccountView.from(user));
        }

        static SignInResponse codeRequired(SentCode code) {
            return new SignInResponse("CODE_REQUIRED", code.sentTo(), code.developmentMode(), null);
        }
    }

    private final AuthService authService;
    private final AccountService accountService;
    private final VerificationCodeService codes;
    private final SessionSignIn sessionSignIn;
    private final SocialClientRegistrations social;

    public AuthController(AuthService authService, AccountService accountService, VerificationCodeService codes,
                          SessionSignIn sessionSignIn, SocialClientRegistrations social) {
        this.authService = authService;
        this.accountService = accountService;
        this.codes = codes;
        this.sessionSignIn = sessionSignIn;
        this.social = social;
    }

    /** Spring Security passes in this session's CSRF token; we hand it to the page. */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @GetMapping("/options")
    public SignInOptions options() {
        return new SignInOptions(social.googleEnabled(), social.appleEnabled());
    }

    /** Creates an account and signs straight in. 201 = "Created". */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public SignInResponse register(@RequestBody RegisterRequest body,
                                   HttpServletRequest request, HttpServletResponse response) {
        AppUser user = authService.register(body.email(), body.password(), body.displayName(), body.phone());
        sessionSignIn.completeSignIn(user, request, response);
        return SignInResponse.signedIn(user);
    }

    /**
     * Step 1: email and password. If two-step verification is on, we send a
     * code and wait; otherwise the person is signed in now.
     */
    @PostMapping("/sign-in")
    public SignInResponse signIn(@RequestBody SignInRequest body,
                                 HttpServletRequest request, HttpServletResponse response) {
        AppUser user = authService.checkPassword(body.email(), body.password());

        if (user.getTwoFactorMethod() == TwoFactorMethod.NONE) {
            sessionSignIn.completeSignIn(user, request, response);
            return SignInResponse.signedIn(user);
        }
        sessionSignIn.startTwoStep(user, request);
        return SignInResponse.codeRequired(codes.send(user, CodePurpose.SIGN_IN));
    }

    /** Step 2: the 6-digit code. */
    @PostMapping("/sign-in/verify")
    public SignInResponse verifyCode(@RequestBody CodeRequest body,
                                     HttpServletRequest request, HttpServletResponse response) {
        AppUser user = pendingUser(request);
        codes.verify(user, CodePurpose.SIGN_IN, body.code());
        sessionSignIn.completeSignIn(user, request, response);
        return SignInResponse.signedIn(user);
    }

    @PostMapping("/sign-in/resend")
    public SignInResponse resendCode(HttpServletRequest request) {
        return SignInResponse.codeRequired(codes.send(pendingUser(request), CodePurpose.SIGN_IN));
    }

    // The account that got its password right and is waiting for a code.
    private AppUser pendingUser(HttpServletRequest request) {
        Long userId = sessionSignIn.pendingUserId(request).orElseThrow(() -> new AuthException(
                HttpStatus.UNAUTHORIZED, "Your sign-in timed out. Please enter your email and password again."));
        return accountService.get(userId);
    }
}
