package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.*;
import com.lostsidewalk.buffy.app.model.error.ErrorDetails;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.FeedDiscoveryException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.ResponseEntity.unprocessableEntity;

@Slf4j
@ControllerAdvice
public class AppErrorHandler {

    @Autowired
    ErrorLogService errorLogService;

    @Autowired
    ConcurrentHashMap<String, Integer> errorStatusMap;

    private void updateErrorCount(Exception e) {
        String n = e.getClass().getSimpleName();
        if (errorStatusMap.containsKey(n)) {
            errorStatusMap.put(n, errorStatusMap.get(n) + 1);
        } else {
            errorStatusMap.put(n, 1);
        }
    }
    //
    // internal server error conditions:
    //
    // database access exception
    // data not found exception
    // feed discovery exception
    // OPML (export) exception
    // IO exception/client abort exception
    // illegal argument exception (runtime)
    // stripe exception
    // mail exception
    //
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<?> handleDataAccessException(DataAccessException e, Authentication authentication) {
        errorLogService.logDataAccessException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return internalServerErrorResponse();
    }

    @ExceptionHandler(DataUpdateException.class)
    public ResponseEntity<?> handleDataUpdateException(DataUpdateException e, Authentication authentication) {
        errorLogService.logDataUpdateException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return internalServerErrorResponse();
    }

    @ExceptionHandler(FeedDiscoveryException.class)
    public ResponseEntity<?> handleFeedDiscoveryException(FeedDiscoveryException e, Authentication authentication) {
        errorLogService.logFeedDiscoveryException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return internalServerErrorResponse();
    }

    @ExceptionHandler(OpmlException.class)
    public ResponseEntity<?> handleOpmlException(OpmlException e, Authentication authentication) {
        errorLogService.logOpmlException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return internalServerErrorResponse();
    }

    @ExceptionHandler(IOException.class)
    public void handleIOException(IOException e, Authentication authentication) {
        String username = ofNullable(authentication).map(Authentication::getName).orElse(null);
        Date timestamp = new Date();
        if (e instanceof ClientAbortException) {
            errorLogService.logClientAbortException(username, timestamp, (ClientAbortException) e);
        } else {
            errorLogService.logIOException(username, timestamp, e);
        }
        updateErrorCount(e);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArumentException(IllegalArgumentException e, Authentication authentication) {
        errorLogService.logIllegalArgumentException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return internalServerErrorResponse();
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<?> handleStripeException(StripeException e, Authentication authentication) {
        errorLogService.logStripeException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return internalServerErrorResponse();
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<?> hanldeMailException(MailException e, Authentication authentication) {
        errorLogService.logMailException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return internalServerErrorResponse();
    }
    //
    // invalid credentials conditions (token-related):
    //
    // token is expired
    // username is missing from token
    // unable to locate authentication token (in request header or cookie)
    // token validation claim is outdated
    // token validation claim is missing
    // unable to parse token
    // not a valid token (claims are missing)
    //
    @ExceptionHandler(TokenValidationException.class)
    public ResponseEntity<?> handleTokenValidationException(TokenValidationException e, Authentication authentication) {
        errorLogService.logTokenValidationException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return invalidCredentialsResponse();
    }
    //
    // invalid credentials conditions (other):
    //
    // supplied credentials are invalid
    // userDao cannot locate user by name
    // userDao cannot locate user by email address
    // user by name NEQ user by email address
    //
    @ExceptionHandler(AuthenticationException.class) // bad credentials exception, username not found, oauth2, etc.
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException e, Authentication authentication) {
        errorLogService.logAuthenticationException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return invalidCredentialsResponse();
    }
    //
    // bad request conditions:
    //
    // invalid method arguments
    // invalid callback from Stripe (signature verification failed)
    // missing/empty authentication claim during login/pw reset/verification
    // incorrect auth provider for user
    // invalid registration request
    // stripe customer exception
    // proxy URL hash validation failure
    //
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, Authentication authentication) {
        errorLogService.logMethodArgumentNotValidException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        String responseMessage = e.getBindingResult().getFieldErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining(","));
        return badRequestResponse("Validation Failed", responseMessage);
    }

    @ExceptionHandler(ValidationException.class) // runtime exception
    protected ResponseEntity<?> handleValidationException(ValidationException e, Authentication authentication) {
        errorLogService.logValidationException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);

        return badRequestResponse("Validation Failed", e.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<?> handleSignatureError(SignatureVerificationException e, Authentication authentication) {
        errorLogService.logSignatureVerificationException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return badRequestResponse("Signature verification failed", e.getSigHeader());
    }

    @ExceptionHandler(AuthClaimException.class)
    public ResponseEntity<?> handleAuthClaimException(AuthClaimException e, Authentication authentication) {
        errorLogService.logAuthClaimException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return badRequestResponse( "Authentication failed", EMPTY);
    }

    @ExceptionHandler(AuthProviderException.class)
    public ResponseEntity<?> handleUserProviderException(AuthProviderException e, Authentication authentication) {
        errorLogService.logAuthProviderException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return badRequestResponse("Authentication failed", EMPTY);
    }

    @ExceptionHandler(RegistrationException.class)
    ResponseEntity<?> handleRegistrationException(RegistrationException e, Authentication authentication) {
        errorLogService.logRegistrationException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return badRequestResponse("Registration failed", e.getMessage());
    }

    @ExceptionHandler(ProxyUrlHashException.class)
    ResponseEntity<?> handleProxyUrlHashException(ProxyUrlHashException e, Authentication authentication) {
        errorLogService.logProxyUrlHashException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return unprocessableEntity().build();
    }
    //
    // utility methods
    //
    private static ResponseEntity<?> internalServerErrorResponse() {
        return new ResponseEntity<>(getErrorDetails( "Something horrible happened, please try again later.", EMPTY), INTERNAL_SERVER_ERROR);
    }
    private static ResponseEntity<?> invalidCredentialsResponse() {
        return new ResponseEntity<>(getErrorDetails("Invalid credentials", EMPTY), FORBIDDEN);
    }

    private static ResponseEntity<?> badRequestResponse(String message, String messageDetails) {
        return new ResponseEntity<>(getErrorDetails(message, messageDetails), BAD_REQUEST);
    }

    private static ErrorDetails getErrorDetails(String message, String detailMessage) {
        return new ErrorDetails(new Date(), message, detailMessage);
    }
}
