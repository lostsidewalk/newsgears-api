package com.lostsidewalk.buffy.app;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.feed.QueueDefinitionService;
import com.lostsidewalk.buffy.app.model.request.FeedStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.QueueConfigRequest;
import com.lostsidewalk.buffy.app.model.request.Subscription;
import com.lostsidewalk.buffy.app.model.response.*;
import com.lostsidewalk.buffy.app.opml.OpmlService;
import com.lostsidewalk.buffy.app.proxy.ProxyService;
import com.lostsidewalk.buffy.app.query.SubscriptionCreationTask;
import com.lostsidewalk.buffy.app.query.SubscriptionDefinitionService;
import com.lostsidewalk.buffy.app.querymetrics.SubscriptionMetricsService;
import com.lostsidewalk.buffy.app.resolution.FeedResolutionService;
import com.lostsidewalk.buffy.app.thumbnail.ThumbnailService;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.model.RenderedThumbnail;
import com.lostsidewalk.buffy.post.PostImporter;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import com.lostsidewalk.buffy.subscription.SubscriptionDefinition;
import com.lostsidewalk.buffy.subscription.SubscriptionMetrics;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.BlockingQueue;

import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.lostsidewalk.buffy.app.ResponseMessageUtils.buildResponseMessage;
import static com.lostsidewalk.buffy.app.user.UserRoles.UNVERIFIED_ROLE;
import static com.lostsidewalk.buffy.app.utils.ThumbnailUtils.getImage;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;
import static org.apache.commons.collections4.MapUtils.size;
import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@Validated
public class QueueDefinitionController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    QueueDefinitionService queueDefinitionService;

    @Autowired
    SubscriptionDefinitionService subscriptionDefinitionService;

    @Autowired
    SubscriptionMetricsService subscriptionMetricsService;

    @Autowired
    ThumbnailService thumbnailService;

    @Autowired
    OpmlService opmlService;

    @Autowired
    ProxyService proxyService;

    @Autowired
    FeedResolutionService feedResolutionService;

    @Autowired
    PostImporter postImporter;

    @Autowired
    Validator validator;

    @Value("${newsgears.thumbnail.size}")
    int thumbnailSize;
    //
    // get feed definitions
    //
    @GetMapping("/queues")
    @Secured({UNVERIFIED_ROLE})
    public ResponseEntity<QueueFetchResponse> getQueues(Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getFeeds for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        // query definitions
        List<SubscriptionDefinition> allSubscriptionDefinitions = subscriptionDefinitionService.findByUsername(username);
        Map<Long, List<ThumbnailedSubscriptionDefinition>> subscriptionDefinitionsByQueueId = new HashMap<>();
        for (SubscriptionDefinition qd : allSubscriptionDefinitions) {
            subscriptionDefinitionsByQueueId.computeIfAbsent(qd.getQueueId(), l -> new ArrayList<>()).add(addThumbnail(qd));
        }
        // query metrics
        List<SubscriptionMetricsWithErrorDetails> allSubscriptionMetrics = subscriptionMetricsService.findByUsername(username).stream()
                .map(q -> SubscriptionMetricsWithErrorDetails.from(q, getQueryExceptionTypeMessage(q.getErrorType())))
                .toList();
        Map<Long, List<SubscriptionMetricsWithErrorDetails>> metricsBySubscriptionDefinitionId = new HashMap<>();
        for (SubscriptionMetricsWithErrorDetails qmEd : allSubscriptionMetrics) {
            metricsBySubscriptionDefinitionId.computeIfAbsent(qmEd.getSubscriptionId(), l -> new ArrayList<>()).add(qmEd);
        }
        // feed definitions
        List<QueueDefinition> feedDefinitions = queueDefinitionService.findByUser(username);
        stopWatch.stop();
        appLogService.logFeedFetch(username, stopWatch, size(feedDefinitions), size(subscriptionDefinitionsByQueueId));
        return ok(
                QueueFetchResponse.from(feedDefinitions, subscriptionDefinitionsByQueueId, metricsBySubscriptionDefinitionId)
        );
    }
    //
    // create feed definitions
    //
    @PostMapping("/queues/")
    @Secured({UNVERIFIED_ROLE})
//    @Transactional
    public ResponseEntity<List<QueueConfigResponse>> createQueue(@RequestBody List<@Valid QueueConfigRequest> queueConfigRequests, Authentication authentication) throws DataAccessException, DataUpdateException, DataConflictException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("createFeed adding {} feeds for user={}", size(queueConfigRequests), username);
        StopWatch stopWatch = StopWatch.createStarted();
        List<QueueDefinition> createdQueues = new ArrayList<>();
        List<QueueConfigResponse> queueConfigResponse = new ArrayList<>();
        // for ea. feed config request
        for (QueueConfigRequest queueConfigRequest : queueConfigRequests) {
            // create the feed
            Long queueId = queueDefinitionService.createFeed(username, queueConfigRequest);
            List<Subscription> subscriptions = queueConfigRequest.getSubscriptions();
            if (isNotEmpty(subscriptions)) {
                try {
                    // partition all RSS/ATOM subscriptions
                    // Note: partition size of 1 just grabs the first first sub in each queue; this is a special case for small hardware
                    // TODO: make the partition size configurable
                    List<List<Subscription>> partitions = Lists.partition(subscriptions, 1);
                    Iterator<List<Subscription>> iter = partitions.iterator();
                    List<Subscription> firstPartition = iter.next();
                    // perform synchronous resolution on the first partition
                    ImmutableMap<String, FeedDiscoveryInfo> discoveryCache = feedResolutionService.resolveIfNecessary(firstPartition);
                    // create the queries (for the first partition)
                    List<SubscriptionDefinition> createdQueries = subscriptionDefinitionService.createQueries(username, queueId, firstPartition);
                    if (isNotEmpty(createdQueries) && isNotEmpty(discoveryCache)) {
                        // perform import-from-cache (first partition only)
                        postImporter.doImport(createdQueries, discoveryCache);
                    }
                    // queue up the remaining partitions
                    iter.forEachRemaining(p -> addToCreationQueue(p, username, queueId));
                } catch (Exception e) {
                    log.warn("Feed initial import failed due to: {}", e.getMessage());
                }
            }

            // re-fetch this feed definition
            QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
            createdQueues.add(queueDefinition);
            // re-fetch query definitions for this feed
            List<ThumbnailedSubscriptionDefinition> subscriptionDefinitions = addThumbnails(subscriptionDefinitionService.findByQueueId(username, queueId));
            // build feed config responses to return the front-end
            queueConfigResponse.add(QueueConfigResponse.from(
                    queueDefinition,
                    subscriptionDefinitions,
                    buildThumbnail(queueDefinition))
            );
        }
        stopWatch.stop();
        appLogService.logFeedCreate(username, stopWatch, size(queueConfigRequests), size(createdQueues));

        return ok(queueConfigResponse);
    }
    //
    // add query definition to feed definition
    //
    @PostMapping("/queues/{queueId}/subscriptions/")
    @Secured({UNVERIFIED_ROLE})
    public ResponseEntity<SubscriptionConfigResponse> addQueries(@RequestBody List<@Valid Subscription> subscriptions, @PathVariable Long queueId, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addQueries adding {} queries for user={}, queueId={}", size(subscriptions), username, queueId);
        StopWatch stopWatch = StopWatch.createStarted();
        for (Subscription subscription : subscriptions) {
            Set<ConstraintViolation<Subscription>> constraintViolations = validator.validate(subscription);
            if (isNotEmpty(constraintViolations)) {
                // TODO: need to enforce one subscription per feed per user
                throw new SubscriptionValidationException(constraintViolations);
            }
        }
        SubscriptionConfigResponse subscriptionConfigResponse = null;
        try {
            // partition all RSS/ATOM subscriptions
            // Note: partition size of 1 just grabs the first first sub in each queue; this is a special case for small hardware
            // TODO: make the partition size configurable
            List<List<Subscription>> partitions = Lists.partition(subscriptions, 1);
            Iterator<List<Subscription>> iter = partitions.iterator();
            List<Subscription> firstPartition = iter.next();
            // perform synchronous resolution on the first partition
            ImmutableMap<String, FeedDiscoveryInfo> discoveryCache = feedResolutionService.resolveIfNecessary(firstPartition);
            // create the queries (for the first partition)
            List<SubscriptionDefinition> createdQueries = subscriptionDefinitionService.createQueries(username, queueId, firstPartition);
            if (isNotEmpty(createdQueries) && isNotEmpty(discoveryCache)) {
                // perform import-from-cache (first partition only)
                postImporter.doImport(createdQueries, discoveryCache);
            }
            // queue up the remaining partitions
            iter.forEachRemaining(p -> addToCreationQueue(p, username, queueId));
            // produce the response
            subscriptionConfigResponse = SubscriptionConfigResponse.from(addThumbnails(createdQueries));
            stopWatch.stop();
            appLogService.logAddQueries(username, stopWatch, size(createdQueries));
        } catch (Exception e) {
            log.warn("Query initial import failed due to: {}", e.getMessage());
        }

        return ok(subscriptionConfigResponse);
    }

    @Autowired
    private BlockingQueue<SubscriptionCreationTask> creationTaskQueue;

    private void addToCreationQueue(List<Subscription> partition, String username, Long queueId) {
        creationTaskQueue.add(new SubscriptionCreationTask(partition, username, queueId));
    }

    private static String getQueryExceptionTypeMessage(SubscriptionMetrics.QueryExceptionType exceptionType) {
        return exceptionType == null ? EMPTY : switch (exceptionType) {
            case FILE_NOT_FOUND_EXCEPTION -> "We weren't able to locate a feed at the URL you provided.";
            case SSL_HANDSHAKE_EXCEPTION -> "We're unable to reach this URL due to a problem with the remote SSL.  Try another protocol, or resolve the issue on the remote system.";
            case UNKNOWN_HOST_EXCEPTION -> "We're unable to resolve the hostname in the URL you provided.";
            case SOCKET_TIMEOUT_EXCEPTION -> "The remote system seems to have timed out; you might want to try to discover this feed later.";
            case SOCKET_EXCEPTION -> "We encountered a problem reading network data from the URL you provided.";
            case CONNECT_EXCEPTION -> "We were unable to connect to the remote system at the URL you provided.";
            case PARSING_FEED_EXCEPTION -> "The feed at the URL you provided has syntax issues that prevent us being able to read it properly.";
            case ILLEGAL_ARGUMENT_EXCEPTION -> "Sorry, we're not able to read the feed at the URL you provided.";
            case PERMANENTLY_REDIRECTED -> "This feed has been permanently moved.  We have additional details.";
            case IO_EXCEPTION, OTHER -> "Something horrible happened while reading the feed at the URL you provided.";
            case UNSECURE_REDIRECT -> "This feed is unsecure, and has been redirected so we've opted not to follow it.";
            case TOO_MANY_REDIRECTS -> "This feed is being redirected too many times.";
            case HTTP_CLIENT_ERROR, HTTP_SERVER_ERROR -> "We encountered an HTTP error fetching the feed at the URL you provided.";
        };
    }
    //
    // update feed definition
    //
    @PutMapping("/queues/{id}")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<QueueConfigResponse> updateQueue(@PathVariable("id") Long id, @Valid @RequestBody QueueConfigRequest queueConfigRequest, Authentication authentication) throws DataAccessException, DataUpdateException, DataConflictException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateFeed for user={}, queueId={}", username, id);
        StopWatch stopWatch = StopWatch.createStarted();
        // (1) update the feed
        queueDefinitionService.updateFeed(username, id, queueConfigRequest);
        // (2) re-fetch this feed definition and query definitions and return to front-end
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        // (3) thumbnail the query definitions
        List<ThumbnailedSubscriptionDefinition> subscriptionDefinitions = addThumbnails(subscriptionDefinitionService.findByQueueId(username, id));
        // (4) thumbnail the feed
        byte[] thumbnail = buildThumbnail(queueDefinition);
        stopWatch.stop();
        appLogService.logFeedUpdate(username, stopWatch, id);
        return ok(QueueConfigResponse.from(
                queueDefinition,
                subscriptionDefinitions,
                thumbnail)
        );
    }
    //
    // update query definition
    //
    @PutMapping("/queues/{queueId}/subscriptions/{subscriptionId}")
    public ResponseEntity<SubscriptionConfigResponse> updateQuery(@PathVariable("queueId") Long queueId, @PathVariable("subscriptionId") Long subscriptionId, @Valid @RequestBody Subscription subscription, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQuery updating queueId={}, subscriptionId={} for user={}", queueId, subscriptionId, username);
        StopWatch stopWatch = StopWatch.createStarted();
        SubscriptionConfigResponse subscriptionConfigResponse = null;
        try {
            // partition all RSS/ATOM subscriptions
            // Note: partition size of 1 just grabs the first first sub in each queue; this is a special case for small hardware
            // TODO: make the partition size configurable
            List<Subscription> firstPartition = singletonList(subscription);
            // perform synchronous resolution on the first partition
            ImmutableMap<String, FeedDiscoveryInfo> discoveryCache = feedResolutionService.resolveIfNecessary(firstPartition);
            // create the queries (for the first partition)
            List<SubscriptionDefinition> toImport = subscriptionDefinitionService.updateSubscriptions(username, queueId, firstPartition);
            if (isNotEmpty(toImport) && isNotEmpty(discoveryCache)) {
                // perform import-from-cache (first partition only)
                postImporter.doImport(toImport, discoveryCache);
            }
            SubscriptionDefinition subscriptionDefinition = subscriptionDefinitionService.findById(username, subscriptionId);
            List<SubscriptionDefinition> updatedSubscriptions = singletonList(subscriptionDefinition);
            // produce the response
            subscriptionConfigResponse = SubscriptionConfigResponse.from(addThumbnails(updatedSubscriptions));
            stopWatch.stop();
            appLogService.logUpdateSubscriptions(username, stopWatch, size(updatedSubscriptions));
        } catch (Exception e) {
            log.warn("Query initial import failed due to: {}", e.getMessage());
        }

        return ok(subscriptionConfigResponse);
    }
    //
    // update feed definition status
    //
    /**
     * ENABLED -- mark the feed for import
     * DISABLED -- un-mark the feed for import
     */
    @PutMapping("/queues/status/{id}")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> updateFeedStatus(@PathVariable("id") Long id, @Valid @RequestBody FeedStatusUpdateRequest feedStatusUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateFeedStatus for user={}, postId={}, feedStatusUpdateRequest={}", username, id, feedStatusUpdateRequest);
        StopWatch stopWatch = StopWatch.createStarted();
        queueDefinitionService.updateFeedStatus(username, id, feedStatusUpdateRequest);
        stopWatch.stop();
        appLogService.logFeedStatusUpdate(username, stopWatch, id, feedStatusUpdateRequest, 1);
        return ok().body(buildResponseMessage("Successfully updated feed Id " + id));
    }
    //
    // preview OPML config
    //
    @PostMapping("/queues/opml")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<OpmlConfigResponse> previewOpmlConfig(@RequestParam("files") MultipartFile[] opmlFiles, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("previewOpmlConfig for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        List<QueueConfigRequest> queueConfigRequests = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (MultipartFile opmlFile : opmlFiles) {
            try (InputStream is = opmlFile.getInputStream()) {
                queueConfigRequests.addAll(opmlService.parseOpmlFile(is));
            } catch (ValidationException e) {
                errors.add(opmlFile.getOriginalFilename() + ": " + e.getMessage());
            } catch (Exception e) {
                log.error("Unable to parse OPML file due to: error={}, name={}", e.getMessage(), opmlFile.getOriginalFilename());
                errors.add(opmlFile.getOriginalFilename() + ": Unable to parse OPML, please select another file or correct these errors in order to continue.");
            }
        }
        stopWatch.stop();
        appLogService.logOpmlPreview(username, stopWatch, getLength(opmlFiles), size(queueConfigRequests), size(errors));

        return ok(OpmlConfigResponse.from(queueConfigRequests, errors));
    }
    //
    // UNUSED
    //
    @PostMapping("/queues/thumbnail")
    @Secured({UNVERIFIED_ROLE})
    public ResponseEntity<ThumbnailConfigResponse> previewThumbnailConfig(@RequestParam("file") MultipartFile imageFile, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("previewThumbnailConfig for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        byte[] image = null;
        List<String> errors = new ArrayList<>();
        try (InputStream is = imageFile.getInputStream()) {
            image = getImage(imageFile.getOriginalFilename(), is.readAllBytes(), this.thumbnailSize);
        } catch (IOException e) {
            errors.add(imageFile.getOriginalFilename() + ": " + e.getMessage());
        }
        try {
            validateThumbnail(image);
        } catch (ValidationException e) {
            errors.add(e.getMessage());
        }
        stopWatch.stop();
        appLogService.logThumbnailPreview(username, stopWatch, size(errors));

        return ok(ThumbnailConfigResponse.from(encodeBase64String(image), errors));
    }

    private void validateThumbnail(byte[] image) {
        if (image == null) {
            throw new ValidationException("This format isn't supported.");
        }
    }
    //
    // UNUSED
    //
    @GetMapping("/queues/thumbnail/random")
    @Secured({UNVERIFIED_ROLE})
    public ResponseEntity<ThumbnailConfigResponse> previewRandomThumbnail(Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("previewRandomThumbnail for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        String randomEncodedImage = thumbnailService.getRandom();
        stopWatch.stop();
        appLogService.logRandomThumbnailPreview(username, stopWatch);
        return ok(ThumbnailConfigResponse.from(randomEncodedImage));

    }
    //
    // delete feed
    //
    @DeleteMapping("/queues/{id}")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> deleteFeedById(@PathVariable Long id, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteFeedById for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        queueDefinitionService.deleteById(username, id);
        stopWatch.stop();
        appLogService.logFeedDelete(username, stopWatch, 1);
        return ok().body(buildResponseMessage("Deleted feed Id " + id));
    }
    //
    // delete query
    //
    @DeleteMapping("/queues/{queueId}/subscriptions/{subscriptionId}")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> deleteSubscriptionById(@PathVariable Long queueId, @PathVariable Long subscriptionId, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueryById for user={}, queueId={}, subscriptionId={}", username, queueId, subscriptionId);
        StopWatch stopWatch = StopWatch.createStarted();
        subscriptionDefinitionService.deleteSubscriptionById(username, queueId, subscriptionId);
        stopWatch.stop();
        appLogService.logQueryDelete(username, stopWatch, 1);
        return ok().body(buildResponseMessage("Deleted query Id " + subscriptionId + ", feed Id " + queueId));
    }
    //
    // get (latest) feed metrics
    //
    @GetMapping("/queues/metrics")
    @Secured({UNVERIFIED_ROLE})
    public ResponseEntity<Map<Long, Date>> getLatestMetrics(Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getLatestMetrics for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        Map<Long, Date> latestQueryMetrics = subscriptionMetricsService.findLatestByUsername(username);
        stopWatch.stop();
        appLogService.logLatestQueryMetricsFetch(username, stopWatch, size(latestQueryMetrics));

        return ok(latestQueryMetrics);
    }

    //

    private List<ThumbnailedSubscriptionDefinition> addThumbnails(List<SubscriptionDefinition> subscriptionDefinitions) {
        List<ThumbnailedSubscriptionDefinition> responses = newArrayListWithCapacity(size(subscriptionDefinitions));
        for (SubscriptionDefinition subscriptionDefinition : subscriptionDefinitions) {
            responses.add(addThumbnail(subscriptionDefinition));
        }
        return responses;
    }

    private ThumbnailedSubscriptionDefinition addThumbnail(SubscriptionDefinition q) {
        String imageProxyUrl = buildThumbnailProxyUrl(q);
        return ThumbnailedSubscriptionDefinition.from(q, imageProxyUrl);
    }

    private String buildThumbnailProxyUrl(SubscriptionDefinition q) {
        if (isNotBlank(q.getImgUrl())) {
            return proxyService.rewriteImageUrl(q.getImgUrl(), EMPTY);
        }

        return null;
    }

    private byte[] buildThumbnail(QueueDefinition f) throws DataAccessException {
        if (isNotBlank(f.getQueueImgSrc())) {
            String transportIdent = f.getQueueImgTransportIdent();
            byte[] image = ofNullable(thumbnailService.getThumbnail(transportIdent)).map(RenderedThumbnail::getImage).orElse(null);
            if (image == null) {
                image = ofNullable(thumbnailService.refreshThumbnailFromSrc(transportIdent, f.getQueueImgSrc()))
                        .map(RenderedThumbnail::getImage)
                        .orElse(null);
            }

            return image;
        }

        return null;
    }

    //

    public static class SubscriptionValidationException extends ValidationException {
        SubscriptionValidationException(Set<ConstraintViolation<Subscription>> constraintViolations) {
            super(constraintViolations.stream().map(ConstraintViolation::getMessage).collect(joining("; ")));
        }
    }
}
