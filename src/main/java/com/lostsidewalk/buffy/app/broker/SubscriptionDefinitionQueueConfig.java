package com.lostsidewalk.buffy.app.broker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class SubscriptionDefinitionQueueConfig {

    private final BlockingQueue<SubscriptionCreationTask> creationTaskQueue = new LinkedBlockingQueue<>();

    @Bean(name="creationTaskQueue")
    BlockingQueue<SubscriptionCreationTask> creationTaskQueue() {
        return creationTaskQueue;
    }
}
