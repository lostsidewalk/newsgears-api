package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.feed.FeedDefinitionService;
import com.lostsidewalk.buffy.app.model.request.FeedConfigRequest;
import com.lostsidewalk.buffy.app.model.request.FeedStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.RssAtomUrl;
import com.lostsidewalk.buffy.app.model.response.FeedConfigResponse;
import com.lostsidewalk.buffy.app.model.response.FeedFetchResponse;
import com.lostsidewalk.buffy.app.model.response.OpmlConfigResponse;
import com.lostsidewalk.buffy.app.model.response.ThumbnailConfigResponse;
import com.lostsidewalk.buffy.app.opml.OpmlService;
import com.lostsidewalk.buffy.app.query.QueryDefinitionService;
import com.lostsidewalk.buffy.app.thumbnail.ThumbnailService;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.FeedDiscoveryException;
import com.lostsidewalk.buffy.feed.FeedDefinition;
import com.lostsidewalk.buffy.model.RenderedThumbnail;
import com.lostsidewalk.buffy.newsapi.NewsApiCategories;
import com.lostsidewalk.buffy.newsapi.NewsApiCountries;
import com.lostsidewalk.buffy.newsapi.NewsApiLanguages;
import com.lostsidewalk.buffy.newsapi.NewsApiSources;
import com.lostsidewalk.buffy.post.PostImporter;
import com.lostsidewalk.buffy.query.QueryDefinition;
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
import java.util.*;
import java.util.stream.Stream;

import static com.lostsidewalk.buffy.app.ResponseMessageUtils.buildResponseMessage;
import static com.lostsidewalk.buffy.app.user.UserRoles.UNVERIFIED_ROLE;
import static com.lostsidewalk.buffy.app.user.UserRoles.VERIFIED_ROLE;
import static com.lostsidewalk.buffy.app.utils.ThumbnailUtils.getImage;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.ArrayUtils.getLength;
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
        List<FeedDefinition> feedDefinitions = feedDefinitionService.findByUser(username);
        Map<String, List<QueryDefinition>> queryDefinitionsByFeed = new HashMap<>();
        for (FeedDefinition f : feedDefinitions) {
            List<QueryDefinition> queryDefinitions = queryDefinitionService.findByFeedId(username, f.getId());
            queryDefinitionsByFeed.put(f.getIdent(), queryDefinitions);
        }
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
        appLogService.logFeedFetch(username, stopWatch, size(feedDefinitions), MapUtils.size(queryDefinitionsByFeed));
        return ok(
                FeedFetchResponse.from(feedDefinitions, queryDefinitionsByFeed, newsApiSourcesMap,
                    List.of(NewsApiCountries.values()),
                    List.of(NewsApiCategories.values()),
                    List.of(NewsApiLanguages.values())));
    }

    @PostMapping("/feeds/")
    @Secured({VERIFIED_ROLE})
//    @Transactional
    public ResponseEntity<List<FeedConfigResponse>> createFeed(@Valid @RequestBody FeedConfigRequest[] feedConfigRequests, Authentication authentication) throws DataAccessException, DataUpdateException, FeedDiscoveryException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("createFeed adding {} feeds for user={}", size(feedConfigRequests), username);
        StopWatch stopWatch = StopWatch.createStarted();
        //
        validateFeedConfigRequests(username, List.of(feedConfigRequests));
        //
        List<QueryDefinition> createdQueries = new ArrayList<>();
        List<FeedDefinition> createdFeeds = new ArrayList<>();
        List<FeedConfigResponse> feedConfigResponses = new ArrayList<>();
        // for ea. feed config request
        for (FeedConfigRequest feedConfigRequest : feedConfigRequests) {
            // create the feed
            Long feedId = feedDefinitionService.createFeed(username, feedConfigRequest);
            // create the queries
            createdQueries.addAll(queryDefinitionService.createQueries(username, feedId, feedConfigRequest));
            // re-fetch this feed definition and query definitions
            FeedDefinition feedDefinition = feedDefinitionService.findByFeedId(username, feedId);
            createdFeeds.add(feedDefinition);
            // (not sure if the query re-fetch is absolutely necessary)
            List<QueryDefinition> queryDefinitions = queryDefinitionService.findByFeedId(username, feedId);
            // build feed config responses to return the front-end
            feedConfigResponses.add(FeedConfigResponse.from(feedDefinition, queryDefinitions, buildThumbnail(feedDefinition)));
        }
        // perform the initial import of any newly-created queries
        if (isNotEmpty(createdQueries)) {
            postImporter.doImport(createdQueries);
            try {
                Thread.sleep(5 * 1000); // 5 second pause for the importer to complete
            } catch (InterruptedException ignored) {}
        }
        stopWatch.stop();
        appLogService.logFeedCreate(username, stopWatch, getLength(feedConfigRequests), size(createdFeeds));

        return ok(feedConfigResponses);
    }

    @PutMapping("/feeds/{id}")
    @Secured({UNVERIFIED_ROLE})
//    @Transactional
    public ResponseEntity<FeedConfigResponse> updateFeed(@PathVariable("id") Long id, @Valid @RequestBody FeedConfigRequest feedConfigRequest, Authentication authentication) throws DataAccessException, DataUpdateException, FeedDiscoveryException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateFeed for user={}, feedId={}", username, id);
        StopWatch stopWatch = StopWatch.createStarted();
        feedDefinitionService.update(username, id, feedConfigRequest);
        List<QueryDefinition> updatedQueries = queryDefinitionService.updateQueries(username, id, feedConfigRequest);
        if (isNotEmpty(updatedQueries)) {
            postImporter.doImport(updatedQueries);
            try {
                Thread.sleep(5 * 1000); // 5 second pause for the importer to complete
            } catch (InterruptedException ignored) {}
        }
        // re-fetch this feed definition and query definitions and return to front-end
        FeedDefinition feedDefinition = feedDefinitionService.findByFeedId(username, id);
        List<QueryDefinition> queryDefinitions = queryDefinitionService.findByFeedId(username, id);
        byte[] thumbnail = buildThumbnail(feedDefinition);
        stopWatch.stop();
        appLogService.logFeedUpdate(username, stopWatch, id);
        return ok(FeedConfigResponse.from(feedDefinition, queryDefinitions, thumbnail));
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
            try {
                feedConfigRequests.addAll(opmlService.parseOpmlFile(opmlFile.getInputStream()));
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
        try {
            image = getImage(imageFile.getOriginalFilename(), imageFile.getInputStream().readAllBytes(), this.thumbnailSize);
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
