package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.auth.AuthService;
import com.lostsidewalk.buffy.app.auth.AuthService.AuthClaimException;
import com.lostsidewalk.buffy.app.mail.MailService;
import com.lostsidewalk.buffy.app.mail.MailService.MailException;
import com.lostsidewalk.buffy.app.model.AppToken;
import com.lostsidewalk.buffy.app.token.TokenService;
import com.lostsidewalk.buffy.app.token.TokenService.JwtUtil;
import com.lostsidewalk.buffy.app.token.TokenService.TokenValidationException;
import com.lostsidewalk.buffy.app.model.request.NewPasswordRequest;
import com.lostsidewalk.buffy.app.model.request.PasswordResetRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;

import static com.lostsidewalk.buffy.app.model.TokenType.PW_RESET;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * This controller handles all password reset functionality.
 */
@Slf4j
@Controller
public class PasswordResetController {

    private static final String INIT_PASSWORD_RESET_DEFAULT_RESPONSE_TEXT = "Ok";

    @Autowired
    AppLogService appLogService;

    @Autowired
    AuthService authService;

    @Autowired
    TokenService tokenService;

    @Autowired
    MailService mailService;

    @Value("${pwreset.error.redirect.url}")
    String pwResetErrorRedirectUrl;

    @Value("${pwreset.continue.redirect.url}")
    String pwResetContinueRedirectUrl;
    //
    // pw reset init (open access)
    //
    @RequestMapping(value = "/pw_reset", method = POST)
    @Transactional
    public ResponseEntity<String> initPasswordReset(@Valid @RequestBody PasswordResetRequest passwordResetRequest) throws AuthClaimException, DataAccessException, DataUpdateException {
        StopWatch stopWatch = StopWatch.createStarted();
        AppToken pwResetToken = authService.initPasswordReset(passwordResetRequest);
        try {
            mailService.sendPasswordResetEmail(passwordResetRequest.getUsername(), pwResetToken);
        } catch (MailException | UsernameNotFoundException ignored) {
            // ignored
        }
        stopWatch.stop();
        appLogService.logPasswordResetInit(passwordResetRequest.getUsername(), stopWatch);

        return ok(INIT_PASSWORD_RESET_DEFAULT_RESPONSE_TEXT);
    }
    //
    // pw reset callback from emailed link (open access)
    //
    @RequestMapping(value = "/pw_reset/{token}", method = GET)
    @Transactional
    public void continuePasswordReset(@PathVariable("token") String token, HttpServletResponse response) throws IOException, TokenValidationException, DataAccessException, DataUpdateException {
        JwtUtil jwtUtil = tokenService.instanceFor(PW_RESET, token); // token w/claims
        if (jwtUtil.isTokenExpired()) {
            response.sendRedirect(this.pwResetErrorRedirectUrl);
        }
        String username = jwtUtil.extractUsername();
        log.info("Password reset continuation received for username={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        authService.continuePasswordReset(username, response);
        stopWatch.stop();
        appLogService.logPasswordResetContinue(username, stopWatch);

        response.sendRedirect(this.pwResetContinueRedirectUrl);
    }
    //
    // pw reset finalize (token validation is done in AuthTokenFilter against the PW_AUTH token, not the APP_AUTH token)
    //
    @RequestMapping(value = "/pw_update", method = RequestMethod.PUT)
    @Transactional
    public ResponseEntity<String> finalizePasswordReset(Authentication authentication, @Valid @RequestBody NewPasswordRequest newPasswordRequest) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        String newPassword = newPasswordRequest.getNewPassword();
        String newPasswordConfirmed = newPasswordRequest.getNewPasswordConfirmed();
        if (!StringUtils.equals(newPassword, newPasswordConfirmed)) {
            return badRequest().body("Password confirmation failed to match");
        }
        StopWatch stopWatch = StopWatch.createStarted();
        authService.finalizePwResetAuthClaim(username);
        authService.updatePassword(username, newPassword);
        stopWatch.stop();
        appLogService.logPasswordResetFinalize(username, stopWatch);

        return ok("Password changed successfully");
    }
}
