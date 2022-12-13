package com.lostsidewalk.buffy.app.order;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StripePaymentHandler {

    void paymentIntentCreated(JsonObject payload) {
        log.debug("Payment intent created, payload={}", payload);
    }

    void paymentIntentSucceeded(JsonObject payload) {
        log.debug("Payment intent succeeded, payload={}", payload);
    }
}
