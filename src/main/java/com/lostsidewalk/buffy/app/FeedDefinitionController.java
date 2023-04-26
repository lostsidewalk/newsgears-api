package com.lostsidewalk.buffy.app;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.feed.FeedDefinitionService;
import com.lostsidewalk.buffy.app.model.request.FeedConfigRequest;
import com.lostsidewalk.buffy.app.model.request.FeedStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.RssAtomUrl;
import com.lostsidewalk.buffy.app.model.response.*;
import com.lostsidewalk.buffy.app.opml.OpmlService;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.app.proxy.ProxyService;
import com.lostsidewalk.buffy.app.query.QueryCreationTask;
import com.lostsidewalk.buffy.app.query.QueryDefinitionService;
import com.lostsidewalk.buffy.app.querymetrics.QueryMetricsService;
import com.lostsidewalk.buffy.app.resolution.FeedResolutionService;
import com.lostsidewalk.buffy.app.thumbnail.ThumbnailService;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.feed.FeedDefinition;
import com.lostsidewalk.buffy.model.RenderedThumbnail;
import com.lostsidewalk.buffy.post.PostImporter;
import com.lostsidewalk.buffy.query.QueryDefinition;
import com.lostsidewalk.buffy.query.QueryMetrics;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.DEPUB_PENDING;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;
import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
public class FeedDefinitionController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    FeedDefinitionService feedDefinitionService;

    @Autowired
    QueryDefinitionService queryDefinitionService;

    @Autowired
    QueryMetricsService queryMetricsService;

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
    @GetMapping("/feeds")
    @Secured({UNVERIFIED_ROLE})
    public ResponseEntity<FeedFetchResponse> getFeeds(Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getFeeds for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        // query definitions
        List<QueryDefinition> allQueryDefinitions = queryDefinitionService.findByUsername(username);
        Map<Long, List<ThumbnailedQueryDefinition>> queryDefinitionsByFeedId = new HashMap<>();
        for (QueryDefinition qd : allQueryDefinitions) {
            queryDefinitionsByFeedId.computeIfAbsent(qd.getFeedId(), l -> new ArrayList<>()).add(addThumbnail(qd));
        }
        // query metrics
        List<QueryMetricsWithErrorDetails> allQueryMetrics = queryMetricsService.findByUsername(username).stream()
                .map(q -> QueryMetricsWithErrorDetails.from(q, getQueryExceptionTypeMessage(q.getErrorType())))
                .toList();
        Map<Long, List<QueryMetricsWithErrorDetails>> queryMetricsByQueryId = new HashMap<>();
        for (QueryMetricsWithErrorDetails qmEd : allQueryMetrics) {
            queryMetricsByQueryId.computeIfAbsent(qmEd.getQueryId(), l -> new ArrayList<>()).add(qmEd);
        }
        // feed definitions
        List<FeedDefinition> feedDefinitions = feedDefinitionService.findByUser(username);
        stopWatch.stop();
        appLogService.logFeedFetch(username, stopWatch, size(feedDefinitions), MapUtils.size(queryDefinitionsByFeedId));
        return ok(
                FeedFetchResponse.from(feedDefinitions, queryDefinitionsByFeedId, queryMetricsByQueryId)
        );
    }

    @PostMapping("/feeds/")
    @Secured({UNVERIFIED_ROLE})
//    @Transactional
    public ResponseEntity<List<FeedConfigResponse>> createFeed(@Valid @RequestBody FeedConfigRequest[] feedConfigRequests, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("createFeed adding {} feeds for user={}", size(feedConfigRequests), username);
        StopWatch stopWatch = StopWatch.createStarted();
        List<FeedDefinition> createdFeeds = new ArrayList<>();
        List<FeedConfigResponse> feedConfigResponses = new ArrayList<>();
        // for ea. feed config request
        for (FeedConfigRequest feedConfigRequest : feedConfigRequests) {
            // create the feed
            Long feedId = feedDefinitionService.createFeed(username, feedConfigRequest);
            List<RssAtomUrl> rssAtomUrls = feedConfigRequest.getRssAtomFeedUrls();
            if (isNotEmpty(rssAtomUrls)) {
                try {
                    // partition all RSS/ATOM subscriptions
                    // Note: partition size of 1 just grabs the first first sub in each queue; this is a special case for small hardware
                    // TODO: make the partition size configurable
                    List<List<RssAtomUrl>> partitions = Lists.partition(rssAtomUrls, 1);
                    Iterator<List<RssAtomUrl>> iter = partitions.iterator();
                    List<RssAtomUrl> firstPartition = iter.next();
                    // perform synchronous resolution on the first partition
                    ImmutableMap<String, FeedDiscoveryInfo> discoveryCache = feedResolutionService.resolveIfNecessary(firstPartition);
                    // create the queries (for the first partition)
                    List<QueryDefinition> createdQueries = queryDefinitionService.createQueries(username, feedId, firstPartition);
                    if (isNotEmpty(createdQueries) && isNotEmpty(discoveryCache)) {
                        // perform import-from-cache (first partition only)
                        postImporter.doImport(createdQueries, discoveryCache);
                    }
                    // queue up the remaining partitions
                    iter.forEachRemaining(p -> addToCreationQueue(p, username, feedId));
                } catch (Exception e) {
                    log.warn("Feed initial import failed due to: {}", e.getMessage());
                }
            }

            // re-fetch this feed definition
            FeedDefinition feedDefinition = feedDefinitionService.findByFeedId(username, feedId);
            createdFeeds.add(feedDefinition);
            // re-fetch query definitions for this feed
            List<ThumbnailedQueryDefinition> queryDefinitions = addThumbnails(queryDefinitionService.findByFeedId(username, feedId));
            // build feed config responses to return the front-end
            feedConfigResponses.add(FeedConfigResponse.from(
                    feedDefinition,
                    queryDefinitions,
                    buildThumbnail(feedDefinition))
            );
        }
        stopWatch.stop();
        appLogService.logFeedCreate(username, stopWatch, getLength(feedConfigRequests), size(createdFeeds));

        return ok(feedConfigResponses);
    }

    @PostMapping("/feeds/{feedId}/queries/")
    @Secured({UNVERIFIED_ROLE})
    public ResponseEntity<QueryConfigResponse> addQueries(@RequestBody List<@Valid RssAtomUrl> rssAtomUrls, @PathVariable Long feedId, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addQueries adding {} queries for user={}, feedId={}", size(rssAtomUrls), username, feedId);
        StopWatch stopWatch = StopWatch.createStarted();
        for (RssAtomUrl rssAtomUrl : rssAtomUrls) {
            Set<ConstraintViolation<RssAtomUrl>> constraintViolations = validator.validate(rssAtomUrl);
            if (isNotEmpty(constraintViolations)) {
                throw new RssAtomUrlValidationException(constraintViolations);
            }
        }
        QueryConfigResponse queryConfigResponse = null;
        try {
            // partition all RSS/ATOM subscriptions
            // Note: partition size of 1 just grabs the first first sub in each queue; this is a special case for small hardware
            // TODO: make the partition size configurable
            List<List<RssAtomUrl>> partitions = Lists.partition(rssAtomUrls, 1);
            Iterator<List<RssAtomUrl>> iter = partitions.iterator();
            List<RssAtomUrl> firstPartition = iter.next();
            // perform synchronous resolution on the first partition
            ImmutableMap<String, FeedDiscoveryInfo> discoveryCache = feedResolutionService.resolveIfNecessary(firstPartition);
            // create the queries (for the first partition)
            List<QueryDefinition> createdQueries = queryDefinitionService.createQueries(username, feedId, firstPartition);
            if (isNotEmpty(createdQueries) && isNotEmpty(discoveryCache)) {
                // perform import-from-cache (first partition only)
                postImporter.doImport(createdQueries, discoveryCache);
            }
            // queue up the remaining partitions
            iter.forEachRemaining(p -> addToCreationQueue(p, username, feedId));
            // produce the response
            queryConfigResponse = QueryConfigResponse.from(addThumbnails(createdQueries));
            stopWatch.stop();
            appLogService.logAddQueries(username, stopWatch, size(createdQueries));
        } catch (Exception e) {
            log.warn("Query initial import failed due to: {}", e.getMessage());
        }

        return ok(queryConfigResponse);
    }

    @Autowired
    private BlockingQueue<QueryCreationTask> creationTaskQueue;

    private void addToCreationQueue(List<RssAtomUrl> partition, String username, Long feedId) {
        creationTaskQueue.add(new QueryCreationTask(partition, username, feedId));
    }

    private static String getQueryExceptionTypeMessage(QueryMetrics.QueryExceptionType exceptionType) {
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

    @PutMapping("/feeds/{id}")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<FeedConfigResponse> updateFeed(@PathVariable("id") Long id, @Valid @RequestBody FeedConfigRequest feedConfigRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateFeed for user={}, feedId={}", username, id);
        StopWatch stopWatch = StopWatch.createStarted();
        // (1) update the feed
        feedDefinitionService.updateFeed(username, id, feedConfigRequest);
        // (2) re-fetch this feed definition and query definitions and return to front-end
        FeedDefinition feedDefinition = feedDefinitionService.findByFeedId(username, id);
        // (3) thumbnail the query definitions
        List<ThumbnailedQueryDefinition> queryDefinitions = addThumbnails(queryDefinitionService.findByFeedId(username, id));
        // (4) thumbnail the feed
        byte[] thumbnail = buildThumbnail(feedDefinition);
        stopWatch.stop();
        appLogService.logFeedUpdate(username, stopWatch, id);
        return ok(FeedConfigResponse.from(
                feedDefinition,
                queryDefinitions,
                thumbnail)
        );
    }

    @PutMapping("/feeds/{feedId}/queries/{queryId}")
    public ResponseEntity<QueryConfigResponse> updateQuery(@PathVariable("feedId") Long feedId, @PathVariable("queryId") Long queryId, @Valid @RequestBody RssAtomUrl rssAtomUrl, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQuery updating feedId={}, queryId={} for user={}", feedId, queryId, username);
        StopWatch stopWatch = StopWatch.createStarted();
        QueryConfigResponse queryConfigResponse = null;
        try {
            // partition all RSS/ATOM subscriptions
            // Note: partition size of 1 just grabs the first first sub in each queue; this is a special case for small hardware
            // TODO: make the partition size configurable
            List<RssAtomUrl> firstPartition = singletonList(rssAtomUrl);
            // perform synchronous resolution on the first partition
            ImmutableMap<String, FeedDiscoveryInfo> discoveryCache = feedResolutionService.resolveIfNecessary(firstPartition);
            // create the queries (for the first partition)
            List<QueryDefinition> toImport = queryDefinitionService.updateQueries(username, feedId, firstPartition);
            if (isNotEmpty(toImport) && isNotEmpty(discoveryCache)) {
                // perform import-from-cache (first partition only)
                postImporter.doImport(toImport, discoveryCache);
            }
            QueryDefinition queryDefinition = queryDefinitionService.findById(username, queryId);
            List<QueryDefinition> updatedQueries = singletonList(queryDefinition);
            // produce the response
            queryConfigResponse = QueryConfigResponse.from(addThumbnails(updatedQueries));
            stopWatch.stop();
            appLogService.logUpdateQueries(username, stopWatch, size(updatedQueries));
        } catch (Exception e) {
            log.warn("Query initial import failed due to: {}", e.getMessage());
        }

        return ok(queryConfigResponse);
    }

    /**
     * ENABLED -- mark the feed for import
     * DISABLED -- un-mark the feed for import
     */
    @PutMapping("/feeds/status/{id}")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> updateFeedStatus(@PathVariable("id") Long id, @Valid @RequestBody FeedStatusUpdateRequest feedStatusUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateFeedStatus for user={}, postId={}, feedStatusUpdateRequest={}", username, id, feedStatusUpdateRequest);
        StopWatch stopWatch = StopWatch.createStarted();
        feedDefinitionService.updateFeedStatus(username, id, feedStatusUpdateRequest);
        stopWatch.stop();
        appLogService.logFeedStatusUpdate(username, stopWatch, id, feedStatusUpdateRequest, 1);
        return ok().body(buildResponseMessage("Successfully updated feed Id " + id));
    }

    @PostMapping("/feeds/opml")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<OpmlConfigResponse> previewOpmlConfig(@RequestParam("files") MultipartFile[] opmlFiles, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("previewOpmlConfig for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        List<FeedConfigRequest> feedConfigRequests = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (MultipartFile opmlFile : opmlFiles) {
            try (InputStream is = opmlFile.getInputStream()) {
                feedConfigRequests.addAll(opmlService.parseOpmlFile(is));
            } catch (ValidationException e) {
                errors.add(opmlFile.getOriginalFilename() + ": " + e.getMessage());
            } catch (Exception e) {
                log.error("Unable to parse OPML file due to: error={}, name={}", e.getMessage(), opmlFile.getOriginalFilename());
                errors.add(opmlFile.getOriginalFilename() + ": Unable to parse OPML, please select another file or correct these errors in order to continue.");
            }
        }
        stopWatch.stop();
        appLogService.logOpmlPreview(username, stopWatch, getLength(opmlFiles), size(feedConfigRequests), size(errors));

        return ok(OpmlConfigResponse.from(feedConfigRequests, errors));
    }

    @PostMapping("/feeds/thumbnail")
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

    @GetMapping("/feeds/thumbnail/random")
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
    @DeleteMapping("/feeds/{id}")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> deleteFeedById(@PathVariable Long id, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteFeedById for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        stagingPostService.updateFeedPubStatus(username, id, DEPUB_PENDING);
        feedDefinitionService.deleteById(username, id);
        stopWatch.stop();
        appLogService.logFeedDelete(username, stopWatch, 1);
        return ok().body(buildResponseMessage("Deleted feed Id " + id));
    }

    @DeleteMapping("/feeds/{feedId}/queries/{queryId}")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> deleteQueryById(@PathVariable Long feedId, @PathVariable Long queryId, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueryById for user={}, feedId={}, queryId={}", username, feedId, queryId);
        StopWatch stopWatch = StopWatch.createStarted();
        queryDefinitionService.deleteQueryById(username, feedId, queryId);
        stopWatch.stop();
        appLogService.logQueryDelete(username, stopWatch, 1);
        return ok().body(buildResponseMessage("Deleted query Id " + queryId + ", feed Id " + feedId));
    }

    //

    private List<ThumbnailedQueryDefinition> addThumbnails(List<QueryDefinition> queryDefinitions) {
        List<ThumbnailedQueryDefinition> responses = newArrayListWithCapacity(size(queryDefinitions));
        for (QueryDefinition queryDefinition : queryDefinitions) {
            responses.add(addThumbnail(queryDefinition));
        }
        return responses;
    }

    private ThumbnailedQueryDefinition addThumbnail(QueryDefinition q) {
        String imageProxyUrl = buildThumbnailProxyUrl(q);
        return ThumbnailedQueryDefinition.from(q, imageProxyUrl);
    }

    private String buildThumbnailProxyUrl(QueryDefinition q) {
        if (isNotBlank(q.getQueryImageUrl())) {
            return proxyService.rewriteImageUrl(q.getQueryImageUrl(), EMPTY);
        }

        return null;
    }

    private byte[] buildThumbnail(FeedDefinition f) throws DataAccessException {
        if (isNotBlank(f.getFeedImgSrc())) {
            String transportIdent = f.getFeedImgTransportIdent();
            byte[] image = ofNullable(thumbnailService.getThumbnail(transportIdent)).map(RenderedThumbnail::getImage).orElse(null);
            if (image == null) {
                image = ofNullable(thumbnailService.refreshThumbnailFromSrc(transportIdent, f.getFeedImgSrc()))
                        .map(RenderedThumbnail::getImage)
                        .orElse(null);
            }

            return image;
        }

        return null;
    }

    //

    public static class RssAtomUrlValidationException extends ValidationException {
        RssAtomUrlValidationException(Set<ConstraintViolation<RssAtomUrl>> constraintViolations) {
            super(constraintViolations.stream().map(ConstraintViolation::getMessage).collect(joining("; ")));
        }
    }
}
