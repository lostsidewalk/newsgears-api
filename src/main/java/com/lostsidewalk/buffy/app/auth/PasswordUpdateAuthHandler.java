package com.lostsidewalk.buffy.app.auth;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.AuthClaimException;
import com.lostsidewalk.buffy.app.audit.TokenValidationException;
import com.lostsidewalk.buffy.app.token.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

import static com.lostsidewalk.buffy.app.model.TokenType.PW_AUTH;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
public class PasswordUpdateAuthHandler {

    @Autowired
    AuthService authService;

    @Autowired
    TokenService tokenService;

    @Autowired
    JwtProcessor jwtProcessor;

    void processPasswordUpdate(HttpServletRequest request) throws TokenValidationException, AuthClaimException, DataAccessException {
        String cValue = authService.getTokenCookieFromRequest(PW_AUTH, request);
        if (isNotBlank(cValue)) {
            TokenService.JwtUtil jwtUtil = tokenService.instanceFor(PW_AUTH, cValue);
            jwtUtil.requireNonExpired();
            String username = jwtUtil.extractUsername();
            if (isNotBlank(username)) { // sanity check
                String pwAuthClaim = authService.requirePwResetAuthClaim(username);
                jwtProcessor.processJwt(jwtUtil, username, pwAuthClaim, cValue);
            } else {
                throw new TokenValidationException("Username is missing from token");
            }
        } else {
            throw new TokenValidationException("Unable to locate authentication token");
        }
    }
}
