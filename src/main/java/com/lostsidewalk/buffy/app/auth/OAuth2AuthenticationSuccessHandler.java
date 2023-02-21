package com.lostsidewalk.buffy.app.auth;

import com.lostsidewalk.buffy.app.utils.CookieUtils;
import com.lostsidewalk.buffy.app.audit.BadRequestException;
import com.lostsidewalk.buffy.app.model.AppToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH_REFRESH;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

@Slf4j
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";

    @Autowired
    AuthService authService;

    @Autowired
    HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Value("${newsgears.development:false}")
    boolean isDevelopment;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        //
        String targetUrl = determineTargetUrl(request, response, authentication);
        //
        if (response.isCommitted()) {
            log.error("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }
        //
        super.clearAuthenticationAttributes(request);
        //
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
        //
        String username = authentication.getName();
        try {
            String authClaim = authService.requireAuthClaim(username);
            AppToken appToken = authService.generateAppToken(APP_AUTH_REFRESH, username, authClaim);
            final Cookie tokenCookie = new CookieBuilder(APP_AUTH_REFRESH.tokenName, appToken.authToken)
                    .setPath("/")
                    .setHttpOnly(true)
                    .setMaxAge(appToken.maxAgeInSeconds)
                    .setSecure(!this.isDevelopment)
                    .build();
            // add app token cookie to response
            response.addCookie(tokenCookie);
        } catch (Exception e) {
            throw new BadRequestException(getRootCauseMessage(e));
        }
        //
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

        Optional<String> redirectUri = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        if (redirectUri.isPresent() && !isAuthorizedRedirectUri(redirectUri.get())) {
            throw new BadRequestException("Sorry! We've got an Unauthorized Redirect URI and can't proceed with the authentication");
        }

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        return UriComponentsBuilder.fromUriString(targetUrl)
                .build().toUriString();
    }

    @Value("${newsgears.authorizedRedirectUris}")
    String[] authorizedRedirectUris;

    private boolean isAuthorizedRedirectUri(String uri) {
        URI clientRedirectUri = URI.create(uri);

        return Arrays.stream(this.authorizedRedirectUris)
                .anyMatch(authorizedRedirectUri -> {
                    // Only validate host and port. Let the clients use different paths if they want to
                    URI authorizedURI = URI.create(authorizedRedirectUri);
                    return authorizedURI.getHost().equalsIgnoreCase(clientRedirectUri.getHost())
                            && authorizedURI.getPort() == clientRedirectUri.getPort();
                });
    }
}
