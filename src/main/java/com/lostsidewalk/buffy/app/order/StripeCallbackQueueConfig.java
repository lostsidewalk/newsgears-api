package com.lostsidewalk.buffy.app.order;

import com.stripe.model.Event;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class StripeCallbackQueueConfig {

    private final BlockingQueue<Event> stripeCallbackQueue = new LinkedBlockingQueue<>();

    @Bean(name="stripeCallbackQueue")
    BlockingQueue<Event> stripeCallbackQueue() {
        return stripeCallbackQueue;
    }
}
