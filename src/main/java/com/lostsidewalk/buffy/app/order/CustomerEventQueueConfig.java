package com.lostsidewalk.buffy.app.order;

import com.lostsidewalk.buffy.customer.CustomerEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class CustomerEventQueueConfig {

    private final BlockingQueue<CustomerEvent> customerEventQueue = new LinkedBlockingQueue<>();

    @Bean(name="customerEventQueue")
    BlockingQueue<CustomerEvent> customerEventQueue() {
        return customerEventQueue;
    }
}
