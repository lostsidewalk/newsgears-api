package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.audit.StripeOrderException;
import com.lostsidewalk.buffy.app.model.request.UpdateSubscriptionRequest;
import com.lostsidewalk.buffy.app.model.response.StripeResponse;
import com.lostsidewalk.buffy.app.model.response.SubscriptionResponse;
import com.lostsidewalk.buffy.app.order.StripeOrderService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import static com.lostsidewalk.buffy.app.model.request.SubscriptionStatus.ACTIVE;
import static com.lostsidewalk.buffy.app.model.request.SubscriptionStatus.CANCELED;
import static com.lostsidewalk.buffy.app.user.UserRoles.UNVERIFIED_ROLE;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
public class OrderController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    private StripeOrderService stripeOrderService;

    @Autowired
    private BlockingQueue<Event> stripeCallbackQueue;
    //
    // order initialization (checkout)
    //
    @PostMapping("/order")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<StripeResponse> initCheckout(Authentication authentication) throws StripeException, DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("initCheckout for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        StripeResponse stripeResponse = stripeOrderService.createCheckoutSession(userDetails.getUsername());
        stopWatch.stop();
        appLogService.logCheckoutSessionCreate(username, stopWatch); // Note: don't log the Stripe response
        return new ResponseEntity<>(stripeResponse, OK);
    }
    //
    // stripe callbacks (webhooks)
    //
    private static final String STRIPE_SIGNATURE_HEADER = "Stripe-Signature";

    @PostMapping("/stripe")
    public void stripeCallback(@RequestBody String payload, HttpServletRequest request, HttpServletResponse response) throws SignatureVerificationException {
        String sigHeader = request.getHeader(STRIPE_SIGNATURE_HEADER);
        stripeCallbackQueue.add(stripeOrderService.constructEvent(sigHeader, payload));
        response.setStatus(OK.value());
    }
    //
    // subscription fetch
    //
    @GetMapping("/subscriptions")
    @Secured({UNVERIFIED_ROLE})
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptions(Authentication authentication) throws StripeException, DataAccessException, StripeOrderException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getSubscriptions for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        List<SubscriptionResponse> subscriptions = stripeOrderService.getSubscriptions(username);
        stopWatch.stop();
        appLogService.logSubscriptionFetch(username, stopWatch, size(subscriptions));
        return new ResponseEntity<>(subscriptions, OK);
    }

    @PutMapping("/subscriptions")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> updateSubscription(@Valid @RequestBody UpdateSubscriptionRequest updateSubscriptionRequest, Authentication authentication) throws StripeException, DataAccessException, StripeOrderException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateSubscription for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        if (updateSubscriptionRequest.getSubscriptionStatus() == CANCELED) {
            stripeOrderService.cancelSubscription(username);
            stopWatch.stop();
            appLogService.logSubscriptionCancel(username, stopWatch);
        } else if (updateSubscriptionRequest.getSubscriptionStatus() == ACTIVE) {
            stripeOrderService.resumeSubscription(username);
            stopWatch.stop();
            appLogService.logSubscriptionResume(username, stopWatch);
        } else {
            return badRequest().build();
        }

        return ok(EMPTY);
    }
}
