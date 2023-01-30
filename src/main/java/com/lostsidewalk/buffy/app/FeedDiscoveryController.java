package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.catalog.CatalogService;
import com.lostsidewalk.buffy.app.discovery.FeedDiscoveryService;
import com.lostsidewalk.buffy.app.model.error.ErrorDetails;
import com.lostsidewalk.buffy.app.thumbnail.ThumbnailService;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.FeedDiscoveryException;
import com.lostsidewalk.buffy.discovery.ThumbnailedFeedDiscovery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

import static com.lostsidewalk.buffy.app.user.UserRoles.UNVERIFIED_ROLE;
import static com.lostsidewalk.buffy.app.user.UserRoles.VERIFIED_ROLE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
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
    ThumbnailService thumbnailService;

    @Autowired
    CatalogService catalogService;

    @GetMapping("/discovery/") // Note: the trailing slash is necessary here due to a bug in Spring
    @Secured({VERIFIED_ROLE})
    public ResponseEntity<?> discoverFeed(@Valid @Size(max = 1024) @RequestParam String url, Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("discoverFeed for user={}, url={}", username, url);
        StopWatch stopWatch = StopWatch.createStarted();
        try {
            FeedDiscoveryInfo feedDiscoveryInfo = feedDiscoveryService.performDiscovery(url);
            ThumbnailedFeedDiscovery thumbnailedFeedDiscoveryResponse = thumbnailService.addThumbnailToResponse(feedDiscoveryInfo);
            stopWatch.stop();
            appLogService.logFeedDiscovery(username, stopWatch, url);
            return ok(thumbnailedFeedDiscoveryResponse);
        } catch (FeedDiscoveryException e) {
            String messageBody = switch (e.exceptionType) {
                case FILE_NOT_FOUND_EXCEPTION -> "We weren't able to locate a feed at the URL you provided.";
                case SSL_HANDSHAKE_EXCEPTION -> "We're unable to reach this URL due to a problem with the remote SSL.  Try another protocol, or resolve the issue on the remote system.";
                case UNKNOWN_HOST_EXCEPTION -> "We're unable to resolve the hostname in the URL you provided.";
                case SOCKET_TIMEOUT_EXCEPTION -> "The remote system seems to have times out; you might want to try to discover this feed later.";
                case SOCKET_EXCEPTION -> "We encountered a problem reading network data from the URL you provided.";
                case CONNECT_EXCEPTION -> "We were unable to connect to the remote system at the URL you provided.";
                case PARSING_FEED_EXCEPTION -> "The feed at the URL you provided has syntax issues that prevent us being able to read it properly.";
                case ILLEGAL_ARGUMENT_EXCEPTION -> "Sorry, we're not able to read the feed at the URL you provided.";
                case IO_EXCEPTION, OTHER -> "Something horrible happened while reading the feed at the URL you provided.";
                case UNSECURE_REDIRECT -> "This feed is unsecure, and has been redirected so we've opted not to follow it.";
                case TOO_MANY_REDIRECTS -> "This feed is being redirected too many times.";
                case HTTP_CLIENT_ERROR, HTTP_SERVER_ERROR -> "We encountered an HTTP error fetching the feed at the URL you provided.";
            };
            return badRequest().body(new ErrorDetails(new Date(), messageBody, EMPTY));
        }
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
