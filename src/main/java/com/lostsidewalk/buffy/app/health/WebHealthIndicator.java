package com.lostsidewalk.buffy.app.health;

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
    ConcurrentHashMap<String, Integer> errorStatusMap;

    @Override
    public Health getHealth(boolean includeDetails) {
        return HealthIndicator.super.getHealth(includeDetails);
    }

    @Override
    public Health health() {
        Map<String, Object> healthDetails = new HashMap<>();
        errorStatusMap.forEach((k, v) -> healthDetails.put(k + "Count", Integer.toString(v)));
        return new Health.Builder()
                .up()
                .withDetails(healthDetails)
                .build();
    }
}
