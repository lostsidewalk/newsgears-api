package com.lostsidewalk.buffy.app.audit;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.auth.AuthService.AuthClaimException;
import com.lostsidewalk.buffy.app.auth.AuthService.AuthProviderException;
import com.lostsidewalk.buffy.app.mail.MailService.MailException;
import com.lostsidewalk.buffy.app.model.exception.StripeEventException;
import com.lostsidewalk.buffy.app.opml.OpmlException;
import com.lostsidewalk.buffy.app.order.StripeOrderService.StripeOrderException;
import com.lostsidewalk.buffy.app.token.TokenService.TokenValidationException;
import com.lostsidewalk.buffy.app.user.RegistrationException;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.FeedDiscoveryException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.IOException;
import java.util.Date;

import static java.lang.System.arraycopy;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j(topic = "appErrorLog")
@Service
public class ErrorLogService {

    public void logDataAccessException(String username, Date timestamp, DataAccessException e) {
        auditError("data-access-exception", "message={}", username, timestamp, e.getMessage());
    }

    public void logDataUpdateException(String username, Date timestamp, DataUpdateException e) {
        auditError("data-not-found-exception", "message={}", username, timestamp, e.getMessage());
    }

    public void logFeedDiscoveryException(String username, Date timestamp, FeedDiscoveryException e) {
        auditError("feed-discovery-exception",
                "message={}, exceptionType={}, feedUrl={}, httpStatusCode={}, httpStatusMessage={}, redirectUrl={}, redirectHttpStatusCode={}, redirectHttpStatusMessage={}",
                username, timestamp, e.getMessage(), e.exceptionType, e.feedUrl, e.httpStatusCode, e.httpStatusMessage, e.redirectUrl, e.redirectHttpStatusCode, e.redirectHttpStatusMessage);
    }

    public void logOpmlException(String username, Date timestamp, OpmlException e) {
        auditError("opml-exception", "message={}", username, timestamp, e.getMessage());
    }

    public void logIOException(String username, Date timestamp, IOException e) {
        auditError("io-exception", "message={}", username, timestamp, e.getMessage());
    }

    public void logIllegalArgumentException(String username, Date timestamp, IllegalArgumentException e) {
        auditError("illegal-argument-exception", "message={}", username, timestamp, e.getMessage());
    }

    // Note: this is an exception from the Stripe SDK
    public void logStripeException(String username, Date timestamp, StripeException e) {
        // TODO: build out the format string
        auditError("stripe-exception", "message={}", username, timestamp, e.getMessage());
    }

    // Note: this is an exception that is thrown when the StripeOrderService blows up when servicing a controller-initiated request
    public void logStripeOrderException(String username, Date timestamp, StripeOrderException e) {
        auditError("stripe-order-exception", "message={}", username, timestamp, e.getMessage());
    }

    // Note: this is an exception that is thrown when one of the Stripe callback handlers blows a gasket
    public void logStripeEventException(String username, Date timestamp, StripeEventException e) {
        auditError("stripe-event-exception", "message={}, payload={}", username, timestamp, e.getMessage(), e.eventPayload);
    }

    public void logTokenValidationException(String username, Date timestamp, TokenValidationException e) {
        auditError("token-validation-exception", "message={}", username, timestamp, e.getMessage());
    }

    public void logAuthenticationException(String username, Date timestamp, AuthenticationException e) {
        auditError("authentication-exception", "message={}", username, timestamp, e.getMessage());
    }

    public void logMethodArgumentNotValidException(String username, Date timestamp, MethodArgumentNotValidException e) {
        // TODO: build out the format string
        auditError("method-argument-not-valid-exception", "message={}", username, timestamp, e.getMessage());
    }

    public void logValidationException(String username, Date timestamp, ValidationException e) {
        auditError("validation-exception", "message={}", username, timestamp, e.getMessage());
    }

    public void logSignatureVerificationException(String username, Date timestamp, SignatureVerificationException e) {
        auditError("signature-verification-exception", "header={}, message={}", username, timestamp, e.getSigHeader(), e.getMessage());
    }

    public void logAuthClaimException(String username, Date timestamp, AuthClaimException e) {
        auditError("auth-claim-exception", "message={}", username, timestamp, e.getMessage());
    }

    public void logAuthProviderException(String username, Date timestamp, AuthProviderException e) {
        auditError("auth-provider-exception", "message={}", username, timestamp, e.getMessage());
    }

    public void logRegistrationException(String username, Date timestamp, RegistrationException e) {
        auditError("registration-exception", "message={}", username, timestamp, e.getMessage());
    }

    public void logMailException(String username, Date timestamp, MailException e) {
        auditError("mail-exception", "message={}", username, timestamp, e.getMessage());
    }

    public void logClientAbortException(String username, Date timestamp, ClientAbortException e) {
        auditError("client-abort-exception", "message={}", username, timestamp, e.getMessage());
    }
    //
    private static void auditError(String logTag, String formatStr, String username, Date timestamp, Object... args) {
        String fullFormatStr = "eventType={}, username={}, timestamp={}";
        if (isNotEmpty(formatStr)) {
            fullFormatStr += (", " + formatStr);
        }
        Object[] allArgs = new Object[args.length + 5];
        allArgs[0] = logTag;
        allArgs[1] = username;
        allArgs[2] = timestamp;
        arraycopy(args, 0, allArgs, 3, args.length);
        log.error(fullFormatStr, allArgs);
    }
}
