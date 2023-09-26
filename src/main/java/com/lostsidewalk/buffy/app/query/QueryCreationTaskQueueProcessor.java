package com.lostsidewalk.buffy.app.query;

import com.google.common.collect.ImmutableMap;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.resolution.FeedResolutionService;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.post.PostImporter;
import com.lostsidewalk.buffy.subscription.SubscriptionDefinition;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;

@Slf4j
@Component
public class QueryCreationTaskQueueProcessor implements DisposableBean, Runnable {

    @Autowired
    BlockingQueue<SubscriptionCreationTask> creationTaskQueue;

    @Autowired
    FeedResolutionService feedResolutionService;

    @Autowired
    SubscriptionDefinitionService subscriptionDefinitionService;

    @Autowired
    PostImporter postImporter;

    private volatile boolean isEnabled = true;

    private Thread thread;

    @PostConstruct
    void postConstruct() {
        log.info("Starting query creation task queue processor");
        this.thread = new Thread(this);
        this.thread.start();
    }

    @Override
    public void run() {
        while (isEnabled) {
            try {
                SubscriptionCreationTask creationTask = creationTaskQueue.take();
                log.info("Processing query creation task for username={}, queueId={}, partitionCt={}",
                        creationTask.getUsername(), creationTask.getQueueId(), size(creationTask.getSubscriptions()));
                ImmutableMap<String, FeedDiscoveryInfo> discoveryCache = feedResolutionService.resolveIfNecessary(creationTask.subscriptions);
                // create the queries (for this partition)
                List<SubscriptionDefinition> createdSubscriptions = subscriptionDefinitionService.addSubscriptions(
                        creationTask.username,
                        creationTask.queueId,
                        creationTask.subscriptions);
                if (isNotEmpty(createdSubscriptions)) {
                    // perform import-from-cache (again, first partition only)
                    postImporter.doImport(createdSubscriptions, ImmutableMap.copyOf(discoveryCache));
                }
            } catch (DataAccessException | DataUpdateException | DataConflictException e) {
                log.error("Unable to create query due to: {}", e.getMessage());
            } catch (InterruptedException e) {
                log.warn("Query creation task queue processor thread interrupted, exiting..");
                throw new RuntimeException(e);
            }
        }
    }

    public Health health() {
        Health.Builder builder = (this.thread != null && this.thread.isAlive() && this.isEnabled) ? Health.up() : Health.down();
        return builder.build();
    }

    @Override
    public void destroy() {
        this.isEnabled = false;
    }
}
