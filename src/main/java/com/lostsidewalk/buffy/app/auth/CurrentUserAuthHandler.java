package com.lostsidewalk.buffy.app.auth;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.AuthClaimException;
import com.lostsidewalk.buffy.app.audit.TokenValidationException;
import com.lostsidewalk.buffy.app.token.TokenService;
import com.lostsidewalk.buffy.app.token.TokenService.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH_REFRESH;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
class CurrentUserAuthHandler {

    @Autowired
    AuthService authService;

    @Autowired
    TokenService tokenService;

    @Autowired
    JwtProcessor jwtProcessor;

    void processCurrentUser(HttpServletRequest request, HttpServletResponse response) throws AuthClaimException, TokenValidationException, DataAccessException {
        String cValue = authService.getTokenCookieFromRequest(APP_AUTH_REFRESH, request);
        if (isNotBlank(cValue)) {
            JwtUtil jwtUtil = tokenService.instanceFor(APP_AUTH_REFRESH, cValue);
            jwtUtil.requireNonExpired();
            String username = jwtUtil.extractUsername();
            if (isNotBlank(username)) {
                String authClaim = authService.requireAuthClaim(username);
                jwtProcessor.processJwt(jwtUtil, username, authClaim, cValue);
                authService.addTokenCookieToResponse(APP_AUTH_REFRESH, username, authClaim, response);
            } else {
                throw new TokenValidationException("Username is missing from token");
            }
        } else {
            throw new TokenValidationException("Unable to validate authentication token");
        }
    }
}
