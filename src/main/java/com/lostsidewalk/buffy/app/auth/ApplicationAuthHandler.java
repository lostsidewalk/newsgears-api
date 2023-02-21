package com.lostsidewalk.buffy.app.auth;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.AuthClaimException;
import com.lostsidewalk.buffy.app.audit.TokenValidationException;
import com.lostsidewalk.buffy.app.token.TokenService;
import com.lostsidewalk.buffy.app.token.TokenService.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Component
class ApplicationAuthHandler {

    @Autowired
    AuthService authService;

    @Autowired
    TokenService tokenService;

    @Autowired
    JwtProcessor jwtProcessor;

    void processAllOthers(HttpServletRequest request, @SuppressWarnings("unused") HttpServletResponse response) throws AuthClaimException, TokenValidationException, DataAccessException {
        String headerAuth = request.getHeader("Authorization");
        if (hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            String jwt = headerAuth.substring(7);
            JwtUtil jwtUtil = tokenService.instanceFor(APP_AUTH, jwt);
            jwtUtil.requireNonExpired();
            String username = jwtUtil.extractUsername();
            jwtProcessor.processJwt(jwtUtil, username, authService.requireAuthClaim(username), jwt);
            log.debug("Logged in username={} via JWT header auth for requestUrl={}, requestMethod={}", username, request.getRequestURL(), request.getMethod());
        } else {
            throw new TokenValidationException("Unable to locate authentication token");
        }
    }
}
