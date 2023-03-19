package com.lostsidewalk.buffy.app.query;

import com.google.common.collect.ImmutableMap;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.resolution.FeedResolutionService;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.post.PostImporter;
import com.lostsidewalk.buffy.query.QueryDefinition;
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
    BlockingQueue<QueryCreationTask> creationTaskQueue;

    @Autowired
    FeedResolutionService feedResolutionService;

    @Autowired
    QueryDefinitionService queryDefinitionService;

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
                QueryCreationTask creationTask = creationTaskQueue.take();
                log.info("Processing query creation task for username={}, feedId={}, partitionCt={}",
                        creationTask.getUsername(), creationTask.getFeedId(), size(creationTask.getRssAtomUrls()));
                ImmutableMap<String, FeedDiscoveryInfo> discoveryCache = feedResolutionService.resolveIfNecessary(creationTask.rssAtomUrls);
                // create the queries (for this partition)
                List<QueryDefinition> createdQueries = queryDefinitionService.addQueries(
                        creationTask.username,
                        creationTask.feedId,
                        creationTask.rssAtomUrls);
                if (isNotEmpty(createdQueries)) {
                    // perform import-from-cache (again, first partition only)
                    postImporter.doImport(createdQueries, ImmutableMap.copyOf(discoveryCache));
                }
            } catch (DataAccessException | DataUpdateException e) {
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
