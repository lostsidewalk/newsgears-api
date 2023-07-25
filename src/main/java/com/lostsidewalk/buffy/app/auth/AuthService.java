package com.lostsidewalk.buffy.app.auth;

import com.lostsidewalk.buffy.*;
import com.lostsidewalk.buffy.app.audit.AuthClaimException;
import com.lostsidewalk.buffy.app.audit.AuthProviderException;
import com.lostsidewalk.buffy.app.model.request.PasswordResetRequest;
import com.lostsidewalk.buffy.app.model.AppToken;
import com.lostsidewalk.buffy.app.token.TokenService;
import com.lostsidewalk.buffy.app.model.TokenType;
import com.lostsidewalk.buffy.auth.AuthProvider;
import com.lostsidewalk.buffy.auth.User;
import com.lostsidewalk.buffy.auth.UserDao;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.lostsidewalk.buffy.app.auth.HashingUtils.sha256;
import static com.lostsidewalk.buffy.app.model.TokenType.*;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Optional.of;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
public class AuthService {

    @Value("${newsgears.development:false}")
    boolean isDevelopment;

    public AppToken initPasswordReset(PasswordResetRequest passwordResetRequest) throws AuthClaimException, UsernameNotFoundException, DataAccessException, DataUpdateException {
        if (passwordResetRequest == null) {
            throw new IllegalArgumentException();
        }
        // (1) locate the user by name or email
        String username = passwordResetRequest.getUsername();
        User userByName = userDao.findByName(username);
        if (userByName == null) {
            // invalid request (username supplied by user cannot be found)
            throw new UsernameNotFoundException(username);
        }
        User userByEmail = null;
        if (isNotBlank(passwordResetRequest.getEmail())) {
            userByEmail = userDao.findByEmailAddress(passwordResetRequest.getEmail());
            if (userByEmail == null) {
                // invalid request (email supplied by user cannot be found)
                throw new UsernameNotFoundException(username);
            }
        }
        if (userByEmail != null && !userByName.equals(userByEmail)) {
            // invalid request (user and email supplied but corresponding users don't match)
            throw new UsernameNotFoundException(username);
        }
        // (2) finalize the current pw reset claim (invalidates all outstanding reset tokens)
        log.info("Finalizing current PW reset claim for username={}, email={}", passwordResetRequest.getUsername(), passwordResetRequest.getEmail());
        finalizePwResetClaim(passwordResetRequest.getUsername());
        // (3) generate and email a new pw reset token
        return generatePasswordResetToken(passwordResetRequest.getUsername());
    }

    public void continuePasswordReset(String username, HttpServletResponse response) throws DataAccessException, DataUpdateException {
        // (5) finalize the current pw reset claim (it's being used right now)
        finalizePwResetClaim(username);
        // (6) finalize/regenerate the pw reset auth claim
        finalizePwResetAuthClaim(username);
        // (7) setup a short-lived logged-in session (add the pw_auth claim cookie to the response)
        User user = userDao.findByName(username);
        if (user != null) {
            addTokenCookieToResponse(TokenType.PW_AUTH, username, user.getPwResetAuthClaim(), response);
        } else {
            throw new UsernameNotFoundException(username);
        }
    }

    @Autowired
    UserDao userDao;

    @Autowired
    TokenService tokenService;

    public void requireAuthProvider(String username, AuthProvider authProvider) throws AuthProviderException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        of(user).map(User::getAuthProvider)
                .filter(a -> a == authProvider)
                .stream()
                .findAny()
                .orElseThrow(() -> new AuthProviderException(username, authProvider, user.getAuthProvider()));
    }

    public String requireAuthClaim(String username) throws AuthClaimException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return of(user).map(User::getAuthClaim)
                .orElseThrow(() -> new AuthClaimException("User has no auth claim"));
    }

    public void finalizeAuthClaim(String username) throws DataAccessException, DataUpdateException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        log.info("Finalizing auth claim for username={}", username);
        String newAuthClaim = randomClaimValue();
        user.setAuthClaim(newAuthClaim);
        userDao.updateAuthClaim(user);
    }

    public void finalizePwResetClaim(String username) throws DataAccessException, DataUpdateException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        log.info("Finalizing PW reset claim for username={}", username);
        String newPwResetClaim = randomClaimValue();
        user.setPwResetClaim(newPwResetClaim);
        userDao.updatePwResetClaim(user);
    }

    public void finalizeVerificationClaim(String username) throws DataAccessException, DataUpdateException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        log.info("Finalizing verification claim for username={}", username);
        String newVerificationClaim = randomClaimValue();
        user.setVerificationClaim(newVerificationClaim);
        userDao.updateVerificationClaim(user);
    }

    public String requirePwResetAuthClaim(String username) throws AuthClaimException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return of(user)
                .map(User::getPwResetAuthClaim)
                .orElseThrow(() -> new AuthClaimException("User has no PW reset auth claim"));
    }

    public void finalizePwResetAuthClaim(String username) throws DataAccessException, DataUpdateException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        log.info("Finalizing PW reset auth claim for username={}", username);
        String newPwResetAuthClaim = randomClaimValue();
        user.setPwResetAuthClaim(newPwResetAuthClaim);
        userDao.updatePwResetAuthClaim(user);
    }

    public void addTokenCookieToResponse(TokenType tokenType, String username, String validationClaim, HttpServletResponse response) {
        AppToken appToken = generateAppToken(tokenType, username, validationClaim);
        final Cookie tokenCookie = new CookieBuilder(tokenType.tokenName, appToken.authToken)
                .setPath("/")
                .setHttpOnly(true)
                .setMaxAge(appToken.maxAgeInSeconds)
                .setSecure(!this.isDevelopment)
                .build();
        // add app token cookie to response
        response.addCookie(tokenCookie);
    }

    public String getTokenCookieFromRequest(TokenType tokenType, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (isNotEmpty(cookies)) {
            for (Cookie c : cookies) {
                if (StringUtils.equals(c.getName(), tokenType.tokenName)) {
                    return c.getValue();
                }
            }
        }

        return null;
    }

    public AppToken generateAuthToken(String username) throws AuthClaimException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        String n = user.getUsername();
        String authClaimSecret = user.getAuthClaim();
        if (isBlank(authClaimSecret)) {
            throw new AuthClaimException("User has no auth claim");
        }

        return generateAppToken(APP_AUTH, n, authClaimSecret);
    }

    public AppToken generatePasswordResetToken(String username) throws AuthClaimException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        String n = user.getUsername();
        String pwResetClaimSecret = user.getPwResetClaim();
        if (isBlank(pwResetClaimSecret)) {
            throw new AuthClaimException("User has no PW reset claim");
        }

        return generateAppToken(PW_RESET, n, pwResetClaimSecret);
    }

    public AppToken generateVerificationToken(String username) throws AuthClaimException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        String n = user.getUsername();
        String verificationClaimSecret = user.getVerificationClaim();
        if (isBlank(verificationClaimSecret)) {
            throw new AuthClaimException("User has no verification claim");
        }

        return generateAppToken(VERIFICATION, n, verificationClaimSecret);
    }

    public void updatePassword(String username, String newPassword) throws DataAccessException, DataUpdateException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        user.setPassword(newPassword);
        userDao.updatePassword(user);
    }

    AppToken generateAppToken(TokenType tokenType, String username, String validationClaim) {
        Map<String, Object> claimsMap = new HashMap<>();
        String validationClaimHash = sha256(validationClaim, defaultCharset());
        claimsMap.put(tokenType.tokenName, validationClaimHash);
        String authToken = tokenService.generateToken(claimsMap, username, tokenType);
        int maxAgeInSeconds = tokenType.maxAgeInSeconds;

        return new AppToken(authToken, maxAgeInSeconds);
    }

    private static String randomClaimValue() {
        return randomAlphanumeric(16);
    }
}
