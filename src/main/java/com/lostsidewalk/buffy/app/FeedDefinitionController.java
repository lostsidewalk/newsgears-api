package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.feed.FeedDefinitionService;
import com.lostsidewalk.buffy.app.model.QueryMetricsWithErrorDetails;
import com.lostsidewalk.buffy.app.model.request.FeedConfigRequest;
import com.lostsidewalk.buffy.app.model.request.FeedStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.RssAtomUrl;
import com.lostsidewalk.buffy.app.model.response.FeedConfigResponse;
import com.lostsidewalk.buffy.app.model.response.FeedFetchResponse;
import com.lostsidewalk.buffy.app.model.response.OpmlConfigResponse;
import com.lostsidewalk.buffy.app.model.response.ThumbnailConfigResponse;
import com.lostsidewalk.buffy.app.opml.OpmlService;
import com.lostsidewalk.buffy.app.query.QueryDefinitionService;
import com.lostsidewalk.buffy.app.querymetrics.QueryMetricsService;
import com.lostsidewalk.buffy.app.thumbnail.ThumbnailService;
import com.lostsidewalk.buffy.feed.FeedDefinition;
import com.lostsidewalk.buffy.model.RenderedThumbnail;
import com.lostsidewalk.buffy.newsapi.NewsApiCategories;
import com.lostsidewalk.buffy.newsapi.NewsApiCountries;
import com.lostsidewalk.buffy.newsapi.NewsApiLanguages;
import com.lostsidewalk.buffy.newsapi.NewsApiSources;
import com.lostsidewalk.buffy.post.PostImporter;
import com.lostsidewalk.buffy.query.QueryDefinition;
import com.lostsidewalk.buffy.query.QueryMetrics;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
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
import java.util.stream.Stream;

import static com.lostsidewalk.buffy.app.ResponseMessageUtils.buildResponseMessage;
import static com.lostsidewalk.buffy.app.user.UserRoles.UNVERIFIED_ROLE;
import static com.lostsidewalk.buffy.app.user.UserRoles.VERIFIED_ROLE;
import static com.lostsidewalk.buffy.app.utils.ThumbnailUtils.getImage;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.*;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
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
    PostImporter postImporter;

    @Value("${newsgears.thumbnail.size}")
    int thumbnailSize;
    //
    // get feed names
    //
    @GetMapping("/feed_idents")
    @Secured({UNVERIFIED_ROLE})
    public ResponseEntity<List<String>> getFeedIdents(Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getFeedIdents for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        List<String> feedIdents = feedDefinitionService.findIdentsByUser(username);
        stopWatch.stop();
        appLogService.logFeedIdentFetch(username, stopWatch, size(feedIdents));
        return ok(feedIdents);
    }
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
        Map<Long, List<QueryDefinition>> queryDefinitionsByFeedId = new HashMap<>();
        for (QueryDefinition qd : allQueryDefinitions) {
            queryDefinitionsByFeedId.computeIfAbsent(qd.getFeedId(), l -> new ArrayList<>()).add(qd);
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
        // NewsApi sources
        Map<NewsApiSources, Map<String, String>> newsApiSourcesMap = Stream.of(NewsApiSources.values())
                .collect(toMap(s -> s, s -> Map.of(
                        "name", s.name,
                        "description", s.description,
                        "url", s.url,
                        "category", s.category,
                        "country", s.country,
                        "language", s.language
                )));
        stopWatch.stop();
        appLogService.logFeedFetch(username, stopWatch, size(feedDefinitions), MapUtils.size(queryDefinitionsByFeedId));
        return ok(
                FeedFetchResponse.from(feedDefinitions, queryDefinitionsByFeedId, queryMetricsByQueryId, newsApiSourcesMap,
                    List.of(NewsApiCountries.values()),
                    List.of(NewsApiCategories.values()),
                    List.of(NewsApiLanguages.values())));
    }

    @PostMapping("/feeds/")
    @Secured({VERIFIED_ROLE})
//    @Transactional
    public ResponseEntity<List<FeedConfigResponse>> createFeed(@Valid @RequestBody FeedConfigRequest[] feedConfigRequests, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("createFeed adding {} feeds for user={}", size(feedConfigRequests), username);
        StopWatch stopWatch = StopWatch.createStarted();
        //
        validateFeedConfigRequests(username, List.of(feedConfigRequests));
        //
        List<FeedDefinition> createdFeeds = new ArrayList<>();
        List<FeedConfigResponse> feedConfigResponses = new ArrayList<>();
        // for ea. feed config request
        for (FeedConfigRequest feedConfigRequest : feedConfigRequests) {
            // create the feed
            Long feedId = feedDefinitionService.createFeed(username, feedConfigRequest);
            // create the queries
            List<QueryDefinition> createdQueries = queryDefinitionService.createQueries(username, feedId, feedConfigRequest);
            Map<Long, List<QueryMetricsWithErrorDetails>> queryMetricsByQueryId = new HashMap<>();
            if (isNotEmpty(createdQueries)) {
                // perform the initial import of any newly-created queries
                postImporter.doImport(createdQueries);
                // gather query metrics for this feed
                List<QueryMetricsWithErrorDetails> allQueryMetrics = queryMetricsService.findByFeedId(username, feedId).stream()
                        .map(q -> QueryMetricsWithErrorDetails.from(q, getQueryExceptionTypeMessage(q.getErrorType())))
                        .toList();
                for (QueryMetricsWithErrorDetails qmEd : allQueryMetrics) {
                    queryMetricsByQueryId.computeIfAbsent(qmEd.getQueryId(), l -> new ArrayList<>()).add(qmEd);
                }
            }
            // re-fetch this feed definition
            FeedDefinition feedDefinition = feedDefinitionService.findByFeedId(username, feedId);
            createdFeeds.add(feedDefinition);
            // re-fetch query definitions for this feed
            List<QueryDefinition> queryDefinitions = queryDefinitionService.findByFeedId(username, feedId);
            // build feed config responses to return the front-end
            feedConfigResponses.add(FeedConfigResponse.from(
                    feedDefinition,
                    queryDefinitions,
                    queryMetricsByQueryId,
                    buildThumbnail(feedDefinition))
            );
        }
        stopWatch.stop();
        appLogService.logFeedCreate(username, stopWatch, getLength(feedConfigRequests), size(createdFeeds));

        return ok(feedConfigResponses);
    }

    private static String getQueryExceptionTypeMessage(QueryMetrics.QueryExceptionType exceptionType) {
        return exceptionType == null ? EMPTY : switch (exceptionType) {
            case FILE_NOT_FOUND_EXCEPTION -> "We weren't able to locate a feed at the URL you provided.";
            case SSL_HANDSHAKE_EXCEPTION -> "We're unable to reach this URL due to a problem with the remote SSL.  Try another protocol, or resolve the issue on the remote system.";
            case UNKNOWN_HOST_EXCEPTION -> "We're unable to resolve the hostname in the URL you provided.";
            case SOCKET_TIMEOUT_EXCEPTION -> "The remote system seems to have times out; you might want to try to discover this feed later.";
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
//    @Transactional
    public ResponseEntity<FeedConfigResponse> updateFeed(@PathVariable("id") Long id, @Valid @RequestBody FeedConfigRequest feedConfigRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateFeed for user={}, feedId={}", username, id);
        StopWatch stopWatch = StopWatch.createStarted();
        //
        feedDefinitionService.update(username, id, feedConfigRequest);
        //
        List<QueryDefinition> updatedQueries = queryDefinitionService.updateQueries(username, id, feedConfigRequest);
        Map<Long, List<QueryMetricsWithErrorDetails>> queryMetricsByQueryId = new HashMap<>();
        if (isNotEmpty(updatedQueries)) {
            // perform the initial import of any updated queries
            postImporter.doImport(updatedQueries);
            List<QueryMetricsWithErrorDetails> allQueryMetrics = queryMetricsService.findByFeedId(username, id).stream()
                    .map(q -> QueryMetricsWithErrorDetails.from(q, getQueryExceptionTypeMessage(q.getErrorType())))
                    .toList();
            for (QueryMetricsWithErrorDetails qmEd : allQueryMetrics) {
                queryMetricsByQueryId.computeIfAbsent(qmEd.getQueryId(), l -> new ArrayList<>()).add(qmEd);
            }
        }
        // re-fetch this feed definition and query definitions and return to front-end
        FeedDefinition feedDefinition = feedDefinitionService.findByFeedId(username, id);
        // query definitions
        List<QueryDefinition> queryDefinitions = queryDefinitionService.findByFeedId(username, id);
        // thumbnail
        byte[] thumbnail = buildThumbnail(feedDefinition);
        stopWatch.stop();
        appLogService.logFeedUpdate(username, stopWatch, id);
        return ok(FeedConfigResponse.from(
                feedDefinition,
                queryDefinitions,
                queryMetricsByQueryId,
                thumbnail)
        );
    }

    /**
     * ENABLED -- mark the feed for import
     * DISABLED -- un-mark the feed for import
     */
    @PutMapping("/feeds/status/{id}")
    @Secured({VERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> updateFeedStatus(@PathVariable("id") Long id, @Valid @RequestBody FeedStatusUpdateRequest feedStatusUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateFeedStatus for user={}, postId={}, feedStatusUpdateRequest={}", username, id, feedStatusUpdateRequest);
        StopWatch stopWatch = StopWatch.createStarted();
        feedDefinitionService.update(username, id, feedStatusUpdateRequest);
        stopWatch.stop();
        appLogService.logFeedStatusUpdate(username, stopWatch, id, feedStatusUpdateRequest, 1);
        return ok().body(buildResponseMessage("Successfully updated feed Id " + id));
    }

    @PostMapping("/feeds/opml")
    @Secured({VERIFIED_ROLE})
    @Transactional
    public ResponseEntity<OpmlConfigResponse> previewOpmlConfig(@RequestParam("files") MultipartFile[] opmlFiles, Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("previewOpmlConfig for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        List<FeedConfigRequest> feedConfigRequests = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (MultipartFile opmlFile : opmlFiles) {
            try (InputStream is = opmlFile.getInputStream()) {
                feedConfigRequests.add(opmlService.parseOpmlFile(is));
            } catch (ValidationException e) {
                errors.add(opmlFile.getOriginalFilename() + ": " + e.getMessage());
            } catch (Exception e) {
                log.error("Unable to parse OPML file due to: error={}, name={}", e.getMessage(), opmlFile.getOriginalFilename());
                errors.add(opmlFile.getOriginalFilename() + ": Unable to parse OPML, please select another file or correct these errors in order to continue.");
            }
        }
        try {
            validateFeedConfigRequests(username, feedConfigRequests);
        } catch (ValidationException e) {
            errors.add(e.getMessage());
        }
        stopWatch.stop();
        appLogService.logOpmlPreview(username, stopWatch, getLength(opmlFiles), size(feedConfigRequests), size(errors));

        return ok(OpmlConfigResponse.from(feedConfigRequests, errors));
    }

    private void validateFeedConfigRequests(String username, List<FeedConfigRequest> feedConfigRequests) throws DataAccessException {
        // check for feed-ident uniqueness across all parsed
        Set<String> uniqueFeedIdents = feedConfigRequests.stream().map(FeedConfigRequest::getIdent).collect(toSet());
        if (size(uniqueFeedIdents) != size(feedConfigRequests)) {
            throw new ValidationException("Feed identifiers must be unique.");
        }
        List<String> existingFeedIdents = feedDefinitionService.findIdentsByUser(username);
        // validate ea. individual feed-ident request
        feedConfigRequests.forEach(f -> this.validateFeedConfigRequest(existingFeedIdents, f));
    }

    private void validateFeedConfigRequest(List<String> existingFeedIdents, FeedConfigRequest feedConfigRequest) {
        //
        Set<String> rssAtomUrls = feedConfigRequest.getRssAtomFeedUrls().stream().map(RssAtomUrl::getFeedUrl).collect(toSet());
        if (size(rssAtomUrls) != size(feedConfigRequest.getRssAtomFeedUrls())) {
            // duplicate RSS URLs
            throw new ValidationException("Upstream RSS/ATOM URLs must be unique within a single feed");
        }
        //
        if (existingFeedIdents.contains(feedConfigRequest.getIdent())) {
            // feed ident already defined
            throw new ValidationException("The feed identifier '" + feedConfigRequest.getIdent() + "' is already defined.");
        }
    }

    @PostMapping("/feeds/thumbnail")
    @Secured({VERIFIED_ROLE})
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
    @Secured({VERIFIED_ROLE})
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
        // TODO: check deployed, undeploy if necessary
        feedDefinitionService.deleteById(username, id);
        stopWatch.stop();
        appLogService.logFeedDelete(username, stopWatch, 1);
        return ok().body(buildResponseMessage("Deleted feed Id " + id));
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
}
