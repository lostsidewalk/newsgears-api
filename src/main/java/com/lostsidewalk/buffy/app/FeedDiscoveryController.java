package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.discovery.FeedDiscoveryException;
import com.lostsidewalk.buffy.app.discovery.FeedDiscoveryService;
import com.lostsidewalk.buffy.app.model.response.FeedDiscoveryImageInfo;
import com.lostsidewalk.buffy.app.model.response.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.app.model.response.ThumbnailedFeedDiscoveryImageResponse;
import com.lostsidewalk.buffy.app.model.response.ThumbnailedFeedDiscoveryResponse;
import com.lostsidewalk.buffy.app.thumbnail.ThumbnailService;
import com.lostsidewalk.buffy.model.Thumbnail;
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

import static com.lostsidewalk.buffy.app.user.UserRoles.VERIFIED_ROLE;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
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

    @GetMapping("/discovery/") // Note: the trailing slash is necessary here due to a bug in Spring
    @Secured({VERIFIED_ROLE})
    public ResponseEntity<?> discoverFeed(@Valid @Size(max=512) @RequestParam String url, Authentication authentication) throws DataAccessException, FeedDiscoveryException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("discoverFeed for user={}, url={}", username, url);
        StopWatch stopWatch = StopWatch.createStarted();
        FeedDiscoveryInfo feedDiscoveryInfo = feedDiscoveryService.performDiscovery(url);
        ThumbnailedFeedDiscoveryResponse thumbnailedFeedDiscoveryResponse = ThumbnailedFeedDiscoveryResponse.from(
                feedDiscoveryInfo,
                addThumbnail(feedDiscoveryInfo.getImage()),
                addThumbnail(feedDiscoveryInfo.getIcon())
        );
        stopWatch.stop();
        appLogService.logFeedDiscovery(username, stopWatch, url);
        return ok(thumbnailedFeedDiscoveryResponse);
    }

    private ThumbnailedFeedDiscoveryImageResponse addThumbnail(FeedDiscoveryImageInfo imageInfo) throws DataAccessException {
        if (imageInfo != null) {
            byte[] image = buildThumbnail(imageInfo);
            return image == null ? null : ThumbnailedFeedDiscoveryImageResponse.from(imageInfo, image);
        }

        return null;
    }

    private byte[] buildThumbnail(FeedDiscoveryImageInfo imageInfo) throws DataAccessException {
        if (isNotBlank(imageInfo.getUrl())) {
            String transportIdent = imageInfo.getTransportIdent();
            byte[] image = ofNullable(thumbnailService.getThumbnail(transportIdent)).map(Thumbnail::getImage).orElse(null);
            if (image == null) {
                image = ofNullable(thumbnailService.refreshThumbnail(transportIdent, imageInfo.getUrl(), 140))
                        .map(Thumbnail::getImage)
                        .orElse(null);
            }
            return image;
        }

        return null;
    }
}
