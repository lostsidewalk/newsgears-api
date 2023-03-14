package com.lostsidewalk.buffy.app.query;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class QueryDefinitionQueueConfig {

    private final BlockingQueue<QueryCreationTask> creationTaskQueue = new LinkedBlockingQueue<>();

    @Bean(name="creationTaskQueue")
    BlockingQueue<QueryCreationTask> creationTaskQueue() {
        return creationTaskQueue;
    }
}
