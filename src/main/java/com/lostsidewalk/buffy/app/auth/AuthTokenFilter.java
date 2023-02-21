package com.lostsidewalk.buffy.app.auth;

import com.google.common.collect.ImmutableSet;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.AuthClaimException;
import com.lostsidewalk.buffy.app.audit.ErrorLogService;
import com.lostsidewalk.buffy.app.audit.TokenValidationException;
import com.lostsidewalk.buffy.app.auth.OptionsAuthHandler.MissingOptionsHeaderException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

@Slf4j
@Component
public class AuthTokenFilter extends OncePerRequestFilter {

	@Autowired
	ErrorLogService errorLogService;

	@Autowired
	OptionsAuthHandler optionsAuthHandler;

	@Autowired
	CurrentUserAuthHandler currentUserAuthHandler;

	@Autowired
	PasswordUpdateAuthHandler passwordUpdateAuthHandler;

	@Autowired
	ApplicationAuthHandler applicationAuthHandler;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
			throws ServletException, IOException {
		String requestPath = getPath(request);
		if (shouldApplyFilter(requestPath)) {
			//
			// the authentication filtering logic works as follows:
			//
			// /currentuser is called by the FE on initialization; if a valid auth refresh cookie is present, this will
			// update the refresh cookie just received and pass through to the AuthenticationController to fetch a
			// short-lived authentication token for follow-on requests
			//
			// all other requests must contain an authorization header with an auth token fetch from /currentuser
			//
			// there is special handling for password updates
			//
			try {
				if (StringUtils.equals(request.getMethod(), "OPTIONS")) {
					optionsAuthHandler.processRequest(request);
				} else if (StringUtils.equals(requestPath, "/currentuser")) {
					currentUserAuthHandler.processCurrentUser(request, response);
				} else if (StringUtils.startsWith(requestPath, "/pw_update")) {
					//
					// pw_reset->POST => password reset init call (no filter)
					// pw_reset->GET => password reset callback (continuation, no filter)
					// pw_update->PUT => password update call (special filter)
					//
					passwordUpdateAuthHandler.processPasswordUpdate(request);
				} else {
					applicationAuthHandler.processAllOthers(request, response);
				}
			} catch (MissingOptionsHeaderException e) {
				log.error("Invalid OPTIONS call for requestUrl={}, request header names: {}", request.getRequestURL(), e.headerNames);
			} catch (TokenValidationException | UsernameNotFoundException ignored) {
				// ignore
			} catch (AuthClaimException e) {
				log.error("Cannot set user authentication for requestUrl={}, requestMethod={}, due to: {}", request.getRequestURL(), request.getMethod(), getRootCauseMessage(e));
			} catch (DataAccessException e) {
				errorLogService.logDataAccessException("sys", new Date(), e);
			}
		}

		filterChain.doFilter(request, response);
	}

	private boolean shouldApplyFilter(String requestPath) {
		return !isOpenServletPath(requestPath);
	}

	private String getPath(HttpServletRequest request) {
		return request.getServletPath();
	}

	private static final ImmutableSet<String> OPEN_PATHS = ImmutableSet.of(
			"/authenticate"
	);

	private static final ImmutableSet<String> OPEN_PATH_PREFIXES = ImmutableSet.of(
			"/pw_reset",
			"/register",
			"/verify",
			"/stripe",
			"/proxy/unsecured"
	);

	private boolean isOpenServletPath(String servletPath) {
		return OPEN_PATHS.contains(servletPath) || OPEN_PATH_PREFIXES.stream().anyMatch(servletPath::startsWith);
	}
}
