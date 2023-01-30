package com.lostsidewalk.buffy.app.health;

import com.lostsidewalk.buffy.app.order.CustomerEventQueueProcessor;
import com.lostsidewalk.buffy.app.order.StripeCallbackQueueProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
class WebHealthIndicator implements HealthIndicator {

    @Autowired
    StripeCallbackQueueProcessor stripeCallbackQueueProcessor;

    @Autowired
    CustomerEventQueueProcessor customerEventQueueProcessor;

    @Autowired
    ConcurrentHashMap<String, Integer> errorStatusMap;

    @Override
    public Health getHealth(boolean includeDetails) {
        return HealthIndicator.super.getHealth(includeDetails);
    }

    @Override
    public Health health() {
        Map<String, Object> healthDetails = new HashMap<>();
        healthDetails.put("stripeCallbackQueueProcessorStatus", this.stripeCallbackQueueProcessor.health());
        healthDetails.put("customerEventQueueProcessorStatus", this.customerEventQueueProcessor.health());
        errorStatusMap.forEach((k, v) -> healthDetails.put(k + "Count", Integer.toString(v)));
        return new Health.Builder()
                .up()
                .withDetails(healthDetails)
                .build();
    }
}
