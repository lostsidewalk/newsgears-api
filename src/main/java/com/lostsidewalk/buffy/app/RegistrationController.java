package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.PostPublisher;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.auth.AuthService;
import com.lostsidewalk.buffy.app.auth.AuthService.AuthClaimException;
import com.lostsidewalk.buffy.app.feed.FeedDefinitionService;
import com.lostsidewalk.buffy.app.mail.MailService;
import com.lostsidewalk.buffy.app.mail.MailService.MailException;
import com.lostsidewalk.buffy.app.model.AppToken;
import com.lostsidewalk.buffy.app.model.request.RegistrationRequest;
import com.lostsidewalk.buffy.app.model.response.RegistartionResponse;
import com.lostsidewalk.buffy.app.token.TokenService;
import com.lostsidewalk.buffy.app.token.TokenService.JwtUtil;
import com.lostsidewalk.buffy.app.token.TokenService.TokenValidationException;
import com.lostsidewalk.buffy.app.user.LocalUserService;
import com.lostsidewalk.buffy.app.user.RegistrationException;
import com.lostsidewalk.buffy.feed.FeedDefinition;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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

import java.io.IOException;
import java.util.List;

import static com.lostsidewalk.buffy.app.model.TokenType.VERIFICATION;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@Slf4j
@Controller
public class RegistrationController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    LocalUserService userService;

    @Autowired
    AuthService authService;

    @Autowired
    MailService mailService;

    @Autowired
    TokenService tokenService;

    @Autowired
    FeedDefinitionService feedDefinitionService;

    @Autowired
    PostPublisher postPublisher;

    @Value("${verification.error.redirect.url}")
    String verificationErrorRedirectUrl;

    @Value("${verification.continue.redirect.url}")
    String verificationContinueRedirectUrl;
    //
    // registration and verification span the following two calls:
    //
    // the initial registration process starts here
    //
    @RequestMapping(path = "/register", method = POST)
    @Transactional
    public ResponseEntity<RegistartionResponse> register(@Valid @RequestBody RegistrationRequest registrationRequest) throws RegistrationException, AuthClaimException, DataAccessException, DataUpdateException {
        //
        // (1) validate the incoming params and create the new user entity
        //
        String username = registrationRequest.getUsername();
        String email = registrationRequest.getEmail();
        String password = registrationRequest.getPassword();
        log.info("Registering username={}, email={}", username, email);
        StopWatch stopWatch = StopWatch.createStarted();
        userService.registerUser(username, email, password);
        //
        // (2) generate claims for the user
        //
        authService.finalizeVerificationClaim(username);
        authService.finalizeAuthClaim(username);
        authService.finalizePwResetClaim(username);
        //
        // (3) generate and send the verification token
        //
        AppToken verificationToken = authService.generateVerificationToken(username);
        log.info("Sending verification email to username={}", username);
        try {
            mailService.sendVerificationEmail(username, verificationToken);
        } catch (UsernameNotFoundException | MailException ignored) {
            // ignored
        }
        stopWatch.stop();
        appLogService.logUserRegistration(username, stopWatch);
        //
        // (4) user registration is complete, respond w/username and password, and http status 200 to trigger authentication
        //
        return ok(new RegistartionResponse(username, password));
    }
    //
    // the verification step is completed when the user clicks the get-back link from their email
    //
    @RequestMapping(path = "/verify/{token}", method = GET)
    @Transactional
    public void verify(@PathVariable String token, HttpServletResponse response) throws TokenValidationException, IOException, DataAccessException, DataUpdateException {
        //
        // (5) validate the supplied token
        //
        JwtUtil jwtUtil = tokenService.instanceFor(VERIFICATION, token); // token w/claims
        if (jwtUtil.isTokenExpired()) {
            response.sendRedirect(this.verificationErrorRedirectUrl);
        }
        //
        // (6) extract username from token and mark user as verified
        //
        String username = jwtUtil.extractUsername();
        log.info("Verification continuation received for username={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        userService.markAsVerified(username);
        //
        // (7) finalize verification claim
        //
        authService.finalizeVerificationClaim(username);
        //
        //
        //
        stopWatch.stop();
        appLogService.logUserVerification(username, stopWatch);
        //
        // (7) user verification is complete, hand-off to front-end
        //
        response.sendRedirect(this.verificationContinueRedirectUrl);
    }

    @RequestMapping(path = "/deregister", method = DELETE)
    @Transactional
    public ResponseEntity<?> deregister(Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.info("De-registering username={}", username);
        //
        StopWatch stopWatch = StopWatch.createStarted();
        List<Long> feedIds = feedDefinitionService.findByUser(username).stream().map(FeedDefinition::getId).toList();
        if (isNotEmpty(feedIds)) {
            for (Long feedId : feedIds) {
                postPublisher.unpublishFeed(username, feedId);
            }
        }
        //
        userService.deregisterUser(username);
        //
        stopWatch.stop();
        appLogService.logUserDeregistration(username, stopWatch);

        return ok(EMPTY);
    }
}
