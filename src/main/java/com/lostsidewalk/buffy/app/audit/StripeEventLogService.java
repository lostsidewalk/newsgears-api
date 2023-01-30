package com.lostsidewalk.buffy.app.audit;

import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j(topic = "stripeEventLog")
@Service
public class StripeEventLogService {

    public void debugUnhandledStripeEvent(Event event, @SuppressWarnings("unused") StripeObject payload) {
        log.debug("Received un-handled callback event from Stripe: live={}, eventType={}, eventId={}", event.getLivemode(), event.getType(), event.getId());
    }

    public void logStripeEvent(Event event, @SuppressWarnings("unused") StripeObject stripeObj) {
        String eventType = event.getType();
        log.debug("Received Stripe callback event, live={}, eventType={}, eventId={}", event.getLivemode(), eventType, event.getId());
    }
}
