package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.auth.AuthService;
import com.lostsidewalk.buffy.app.auth.AuthService.AuthClaimException;
import com.lostsidewalk.buffy.app.auth.AuthService.AuthProviderException;
import com.lostsidewalk.buffy.app.user.LocalUserService;
import com.lostsidewalk.buffy.app.model.request.LoginRequest;
import com.lostsidewalk.buffy.app.model.response.LoginResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import static com.lostsidewalk.buffy.AuthProvider.LOCAL;
import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH_REFRESH;
import static com.lostsidewalk.buffy.app.user.UserRoles.SUBSCRIBER_AUTHORITY;
import static com.lostsidewalk.buffy.app.user.UserRoles.UNVERIFIED_ROLE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * This controller handles authentication functionality (i.e., login, logout).
 *
 * Password reset, user registration, and email validation are handled elsewhere.
 *
 * The 'currentUser' call is used by the front-end to determine if the user
 * has a valid refresh token.
 *
 * The 'authenticate' call is used to setup a logged-in session.  This call also adds a
 * refresh token cookie to the response, so that the user remains 'logged in' as long as
 * the cookie persists.
 *
 * The 'deauthenticate' call is used to log out.  Calling this method finalized the auth claim
 * on the user object, so that the further attempts to validate already-generated tokens will fail.
 */
@Slf4j
@Controller
class AuthenticationController {

    @Autowired
    AuthService authService;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    LocalUserService localUserService;
    //
    // auth check
    //
    @Secured({UNVERIFIED_ROLE})
    @RequestMapping(value = "/currentuser", method = GET)
    public ResponseEntity<LoginResponse> getCurrentUser(Authentication authentication) throws AuthClaimException, DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        LoginResponse authenticationResponse = buildAuthenticationResponse(
                authService.generateAuthToken(username).authToken,
                username,
                userDetails.getAuthorities().contains(SUBSCRIBER_AUTHORITY)
        );

        return ok(authenticationResponse);
    }
    //
    // login (open access)
    //
    @RequestMapping(value = "/authenticate", method = POST)
    @Transactional
    public ResponseEntity<LoginResponse> createAuthenticationToken(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response)
            throws BadCredentialsException, UsernameNotFoundException, AuthProviderException, AuthClaimException, DataAccessException {
        // extract username
        String username = loginRequest.getUsername();
        // validate the auth provider
        authService.requireAuthProvider(username, LOCAL);
        // add refresh token cookie to response
        String authClaim = authService.requireAuthClaim(username);
        // authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));
        // generate the auth token/build auth response
        authService.addTokenCookieToResponse(APP_AUTH_REFRESH, username, authClaim, response);
        LoginResponse authenticationResponse = buildAuthenticationResponse(
                authService.generateAuthToken(username).authToken,
                username,
                localUserService.loadUserByUsername(username).getAuthorities().contains(SUBSCRIBER_AUTHORITY)
        );

        log.info("Login succeeded for username={}", username);

        return ok(authenticationResponse);
    }

    private LoginResponse buildAuthenticationResponse(String authToken, String username, boolean hasSubscription) {
        LoginResponse loginResponse;
        loginResponse = LoginResponse.from(authToken, username, hasSubscription);

        return loginResponse;
    }
    //
    // logout (open access)
    //
    @RequestMapping(value = "/deauthenticate", method = GET)
    @Transactional
    public ResponseEntity<String> deauthenticate(Authentication authentication) throws DataAccessException, DataUpdateException {
        if (authentication != null) {
            UserDetails userDetails = (UserDetails) authentication.getDetails();
            String username = userDetails.getUsername();
            // finalize the auth claim
            authService.finalizeAuthClaim(username);
            log.info("Finalized auth claim for username={}", username);
        }

        return ok(EMPTY);
    }
}
