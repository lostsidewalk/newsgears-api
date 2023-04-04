package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.catalog.CatalogService;
import com.lostsidewalk.buffy.app.discovery.FeedDiscoveryService;
import com.lostsidewalk.buffy.app.model.error.UpstreamErrorDetails;
import com.lostsidewalk.buffy.app.model.request.FeedDiscoveryRequest;
import com.lostsidewalk.buffy.app.proxy.ProxyService;
import com.lostsidewalk.buffy.app.resolution.FeedResolutionService;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryImageInfo;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.FeedDiscoveryException;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.FeedDiscoveryExceptionType;
import com.lostsidewalk.buffy.discovery.ThumbnailedFeedDiscovery;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

import static com.lostsidewalk.buffy.app.user.UserRoles.UNVERIFIED_ROLE;
import static com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.FeedDiscoveryExceptionType.PARSING_FEED_EXCEPTION;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
public class FeedDiscoveryController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    FeedDiscoveryService feedDiscoveryService;

    @Autowired
    FeedResolutionService feedResolutionService;

    @Autowired
    CatalogService catalogService;

    @GetMapping("/discovery/collection/{collectionName}")
    public ResponseEntity<?> discoverCollection(@PathVariable String collectionName, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails == null ? "(none)" : userDetails.getUsername();
        log.debug("discoverCollection for user={}, collectionName={}", username, collectionName);
        StopWatch stopWatch = StopWatch.createStarted();
        List<FeedDiscoveryInfo> collectionDiscoveryResponse = feedDiscoveryService.getCollection(collectionName);
        List<ThumbnailedFeedDiscovery> thumbnailedCollectionDiscoveryResponse = null;
        if (isNotEmpty(collectionDiscoveryResponse)) {
            thumbnailedCollectionDiscoveryResponse = collectionDiscoveryResponse.stream().map(this::addThumbnailToResponse).toList();
        }
        stopWatch.stop();
        appLogService.logCollectionFetch(username, stopWatch, collectionName);
        return ok(thumbnailedCollectionDiscoveryResponse);
    }

    @PostMapping("/discovery")
    @Secured({UNVERIFIED_ROLE})
    public ResponseEntity<?> discoverFeed(@Valid @RequestBody FeedDiscoveryRequest feedDiscoveryRequest, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("discoverFeed for user={}, feedDiscoveryRequest={}", username, feedDiscoveryRequest);
        StopWatch stopWatch = StopWatch.createStarted();
        String discoveryUrl = feedDiscoveryRequest.getUrl();
        String discoveryUsername = feedDiscoveryRequest.getUsername();
        String discoveryPassword = feedDiscoveryRequest.getPassword();
        try {
            // perform discovery
            FeedDiscoveryInfo feedDiscoveryInfo = feedDiscoveryService.performDiscovery(discoveryUrl, discoveryUsername, discoveryPassword);
            //
            ThumbnailedFeedDiscovery thumbnailedFeedDiscoveryResponse = addThumbnailToResponse(feedDiscoveryInfo);
            stopWatch.stop();
            appLogService.logFeedDiscovery(username, stopWatch, discoveryUrl);
            return ok(thumbnailedFeedDiscoveryResponse);
        } catch (FeedDiscoveryException e) {
            if (e.exceptionType == PARSING_FEED_EXCEPTION) {
                try {
                    String resolvedUrl = feedResolutionService.performResolution(discoveryUrl);
                    if (isNotBlank(resolvedUrl) && !StringUtils.equals(resolvedUrl, discoveryUrl)) {
                        FeedDiscoveryInfo feedDiscoveryInfo = feedDiscoveryService.performDiscovery(resolvedUrl, discoveryUsername, discoveryPassword);
                        ThumbnailedFeedDiscovery thumbnailedFeedDiscoveryResponse = addThumbnailToResponse(feedDiscoveryInfo);
                        stopWatch.stop();
                        appLogService.logFeedDiscovery(username, stopWatch, resolvedUrl);
                        // (the resolved path is different from the original path, and has been discovered successfully)
                        return ok(thumbnailedFeedDiscoveryResponse);
                    }
                    // (the resolved path is the same as the original path, which has error-ed out)
                    return badRequest().body(
                            new UpstreamErrorDetails(new Date(), getFeedDiscoveryExceptionTypeMessage(e.exceptionType), e.httpStatusCode, e.httpStatusMessage, e.redirectUrl, e.redirectHttpStatusCode, e.redirectHttpStatusMessage));
                } catch (FeedDiscoveryException e1) {
                    // (the resolved path is different from the original path, but has error-ed out)
                    return badRequest().body(
                            new UpstreamErrorDetails(new Date(), getFeedDiscoveryExceptionTypeMessage(e1.exceptionType), e1.httpStatusCode, e1.httpStatusMessage, e1.redirectUrl, e1.redirectHttpStatusCode, e1.redirectHttpStatusMessage));
                }
            }
            // (the original path error-ed out due to a reason other than feed resolution)
            return badRequest().body(
                    new UpstreamErrorDetails(new Date(), getFeedDiscoveryExceptionTypeMessage(e.exceptionType), e.httpStatusCode, e.httpStatusMessage, e.redirectUrl, e.redirectHttpStatusCode, e.redirectHttpStatusMessage));
        }
    }

    public ThumbnailedFeedDiscovery addThumbnailToResponse(FeedDiscoveryInfo feedDiscoveryInfo) {
        //
        FeedDiscoveryImageInfo imageInfo = feedDiscoveryInfo.getImage();
        secureFeedDiscoveryImageInfo(imageInfo);
        //
        FeedDiscoveryImageInfo iconInfo = feedDiscoveryInfo.getIcon();
        secureFeedDiscoveryImageInfo(iconInfo);
        //
        return ThumbnailedFeedDiscovery.from(
                feedDiscoveryInfo,
                imageInfo,
                iconInfo
        );
    }

    @Autowired
    ProxyService proxyService;

    private void secureFeedDiscoveryImageInfo(FeedDiscoveryImageInfo feedDiscoveryImageInfo) {
        if (feedDiscoveryImageInfo != null) {
            String originalUrl = feedDiscoveryImageInfo.getUrl();
            String newUrl = proxyService.rewriteImageUrl(originalUrl, null);

            feedDiscoveryImageInfo.setUrl(newUrl);
        }
    }

    private static String getFeedDiscoveryExceptionTypeMessage(FeedDiscoveryExceptionType exceptionType) {
        return switch (exceptionType) {
            case FILE_NOT_FOUND_EXCEPTION -> "We weren't able to locate a feed at the URL you provided.";
            case SSL_HANDSHAKE_EXCEPTION -> "We're unable to reach this URL due to a problem with the remote SSL.  Try another protocol, or resolve the issue on the remote system.";
            case UNKNOWN_HOST_EXCEPTION -> "We're unable to resolve the hostname in the URL you provided.";
            case SOCKET_TIMEOUT_EXCEPTION -> "The remote system seems to have timed out; you might want to try to discover this feed later.";
            case SOCKET_EXCEPTION -> "We encountered a problem reading network data from the URL you provided.";
            case CONNECT_EXCEPTION -> "We were unable to connect to the remote system at the URL you provided.";
            case PARSING_FEED_EXCEPTION -> "The feed at the URL you provided has syntax issues that prevent us being able to read it properly.";
            case ILLEGAL_ARGUMENT_EXCEPTION -> "Sorry, we're not able to read the feed at the URL you provided.";
            case IO_EXCEPTION, OTHER -> "Something horrible happened while reading the feed at the URL you provided.";
            case UNSECURE_REDIRECT -> "This feed is unsecure, and has been redirected so we've opted not to follow it.";
            case TOO_MANY_REDIRECTS -> "This feed is being redirected too many times.";
            case HTTP_CLIENT_ERROR, HTTP_SERVER_ERROR -> "We encountered an HTTP error fetching the feed at the URL you provided.";
        };
    }

    @GetMapping("/catalog")
    @Secured({UNVERIFIED_ROLE})
    public ResponseEntity<List<ThumbnailedFeedDiscovery>> getCatalog(Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getCatalog for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        List<ThumbnailedFeedDiscovery> catalog = catalogService.getCatalog();
        stopWatch.stop();
        appLogService.logCatalogFetch(username, stopWatch);
        return ok(catalog);
    }
}
