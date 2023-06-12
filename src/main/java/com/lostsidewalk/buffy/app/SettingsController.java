package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.request.DisplaySettingsUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.SettingsUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.UpdateSubscriptionRequest;
import com.lostsidewalk.buffy.app.model.response.DisplaySettingsResponse;
import com.lostsidewalk.buffy.app.model.response.SettingsResponse;
import com.lostsidewalk.buffy.app.model.response.StripeResponse;
import com.lostsidewalk.buffy.app.model.response.SubscriptionResponse;
import com.lostsidewalk.buffy.app.order.StripeOrderService;
import com.lostsidewalk.buffy.app.settings.SettingsService;
import com.stripe.exception.StripeException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.lostsidewalk.buffy.app.model.request.SubscriptionStatus.ACTIVE;
import static com.lostsidewalk.buffy.app.model.request.SubscriptionStatus.CANCELED;
import static com.lostsidewalk.buffy.app.user.UserRoles.UNVERIFIED_ROLE;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@Validated
public class SettingsController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    SettingsService settingsService;

    @Autowired
    StripeOrderService stripeOrderService;

    @Secured({UNVERIFIED_ROLE})
    @GetMapping("/settings/display")
    public ResponseEntity<DisplaySettingsResponse> getDisplaySettings(Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        StopWatch stopWatch = StopWatch.createStarted();
        DisplaySettingsResponse displaySettingsResponse = settingsService.getDisplaySettings(username);
        stopWatch.stop();
        appLogService.logDisplaySettingsFetch(username, stopWatch);
        //
        return ok(displaySettingsResponse);
    }

    @Secured({UNVERIFIED_ROLE})
    @Transactional
    @PutMapping("/settings/display")
    public ResponseEntity<?> updateDisplaySettings(@Valid @RequestBody DisplaySettingsUpdateRequest displaySettingsUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        StopWatch stopWatch = StopWatch.createStarted();
        settingsService.updateDisplaySettings(username, displaySettingsUpdateRequest);
        stopWatch.stop();
        appLogService.logDisplaySettingsUpdate(username, stopWatch);
        return ok(EMPTY);
    }

    @Secured({UNVERIFIED_ROLE})
    @GetMapping("/settings")
    public ResponseEntity<SettingsResponse> getSettings(Authentication authentication) throws DataAccessException, StripeException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        StopWatch stopWatch = StopWatch.createStarted();
        SettingsResponse settingsResponse = settingsService.getFrameworkConfig(username);
        stopWatch.stop();
        appLogService.logSettingsFetch(username, stopWatch);
        //
        stopWatch = StopWatch.createStarted();
        List<SubscriptionResponse> subscriptions = stripeOrderService.getSubscriptions(username);
        stopWatch.stop();
        appLogService.logSubscriptionFetch(username, stopWatch, size(subscriptions));
        if (isNotEmpty(subscriptions)) {
            settingsResponse.setSubscription(subscriptions.get(0));
        }
        return ok(settingsResponse);
    }

    @Secured({UNVERIFIED_ROLE})
    @Transactional
    @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(@Valid @RequestBody SettingsUpdateRequest settingsUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        StopWatch stopWatch = StopWatch.createStarted();
        settingsService.updateFrameworkConfig(username, settingsUpdateRequest);
        stopWatch.stop();
        appLogService.logSettingsUpdate(username, stopWatch, settingsUpdateRequest);
        return ok(EMPTY);
    }
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

    @PutMapping("/subscriptions")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> updateSubscription(@Valid @RequestBody UpdateSubscriptionRequest updateSubscriptionRequest, Authentication authentication) throws StripeException, DataAccessException {
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
