package com.lostsidewalk.buffy.app.thumbnail;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.RenderedThumbnailDao;
import com.lostsidewalk.buffy.app.audit.ErrorLogService;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryImageInfo;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.discovery.ThumbnailedFeedDiscovery;
import com.lostsidewalk.buffy.discovery.ThumbnailedFeedDiscoveryImage;
import com.lostsidewalk.buffy.model.RenderedThumbnail;
import com.lostsidewalk.buffy.thumbnail.ThumbnailDao;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;

import static com.lostsidewalk.buffy.app.utils.ThumbnailUtils.getImage;
import static java.util.Optional.ofNullable;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
public class ThumbnailService {

    @Autowired
    ErrorLogService errorLogService;

    @Autowired
    RenderedThumbnailDao renderedThumbnailDao; // for redis interaction

    public RenderedThumbnail getThumbnail(String transportIdent) throws DataAccessException {
        if (isBlank(transportIdent)) {
            return null;
        }
        log.debug("Attempting to locate thumbnail at transportIdent={}", transportIdent);
        RenderedThumbnail thumbnail = renderedThumbnailDao.findThumbnailByTransportIdent(transportIdent);
        if (thumbnail != null) {
            log.debug("Thumbnail located at transportIdent={}", transportIdent);
        } else {
            log.debug("Unable to locate thumbnail at transportIdent={}", transportIdent);
        }

        return thumbnail;
    }

    @Cacheable("thumbnailRefreshCache")
    public RenderedThumbnail refreshThumbnail(String transportIdent, String imgUrl, int targetSize) {
        log.info("Refreshing thumbnail cache, imgUrl={} @ transportIdent={}", imgUrl, transportIdent);
        RenderedThumbnail thumbnail = null;
        try {
            byte[] imageBytes = getImage(imgUrl, fetch(imgUrl), targetSize);
            if (imageBytes == null) {
                log.error("Failed to decode image at imgUrl={} @ transportIdent={} due to unknown format", imgUrl, transportIdent);
            }
            thumbnail = RenderedThumbnail.from(transportIdent, imageBytes);
            renderedThumbnailDao.putThumbnailAtTransportIdent(transportIdent, thumbnail);
            log.debug("Thumbnail cache updated for imgUrl={} @ transportIdent={}", imgUrl, transportIdent);
        } catch (Exception e) {
            log.warn("Failed to update thumbnail cache for imgUrl={} @ transportIdent={} due to: {}",
                    imgUrl, transportIdent, e.getMessage());
        }

        return thumbnail;
    }

    private byte[] fetch(String url) throws IOException {
        URL u = new URL(url);
        URLConnection urlConnection = u.openConnection();
        // TODO: make this property-configurable
        String userAgent = "Lost Sidewalk FeedGears RSS Aggregator v.0.4";
        urlConnection.setRequestProperty("User-Agent", userAgent);
        try (InputStream is = urlConnection.getInputStream()) {
            return is.readAllBytes();
        }
    }

    public RenderedThumbnail refreshThumbnailFromSrc(String transportIdent, String feedImgSrc) throws DataAccessException {
        byte[] imageBytes = decodeBase64(feedImgSrc);
        if (imageBytes == null) {
            log.error("Failed to decode image with imgSrc={} @ transportIdent={} due to unknown format", feedImgSrc, transportIdent);
        }
        RenderedThumbnail thumbnail = RenderedThumbnail.from(transportIdent, imageBytes);
        renderedThumbnailDao.putThumbnailAtTransportIdent(transportIdent, thumbnail);

        return thumbnail;
    }

    //
    //
    //

    @Autowired
    ThumbnailDao thumbnailDao;

    @Value("${newsgears.thumbnail.size}")
    int thumbnailSize;

    @PostConstruct
    public void postConstruct() {
        try {
            StopWatch stopWatch = StopWatch.createStarted();
            List<String> cache = getThumbnailCache();
            stopWatch.stop();
            log.info("Thumbnail cache initialized: size={}, startTime={}, endTime={}, duration={}",
                    size(cache), stopWatch.getStartTime(), stopWatch.getStopTime(), stopWatch.getTime());
        } catch (DataAccessException e) {
            errorLogService.logDataAccessException("sys", new Date(), e);
        }
    }

    public String getRandom() throws DataAccessException {
        List<String> cache = getThumbnailCache();
        int randIdx = nextInt(0, cache.size());
        return cache.get(randIdx);
    }

    @Cacheable(value = "thumbnailCache")
    List<String> getThumbnailCache() throws DataAccessException {
        return thumbnailDao.findAll();
    }

    @Cacheable(value = "thumbnailedDiscoveryCache")
    public ThumbnailedFeedDiscovery addThumbnailToResponse(FeedDiscoveryInfo feedDiscoveryInfo) throws DataAccessException {
        return ThumbnailedFeedDiscovery.from(
                feedDiscoveryInfo,
                addThumbnail(feedDiscoveryInfo.getImage()),
                addThumbnail(feedDiscoveryInfo.getIcon())
        );
    }

    private ThumbnailedFeedDiscoveryImage addThumbnail(FeedDiscoveryImageInfo imageInfo) throws DataAccessException {
        if (imageInfo != null) {
            byte[] image = buildThumbnail(imageInfo);
            return image == null ? null : ThumbnailedFeedDiscoveryImage.from(imageInfo, image);
        }

        return null;
    }

    private byte[] buildThumbnail(FeedDiscoveryImageInfo imageInfo) throws DataAccessException {
        if (isNotBlank(imageInfo.getUrl())) {
            String transportIdent = imageInfo.getTransportIdent();
            byte[] image = ofNullable(getThumbnail(transportIdent)).map(RenderedThumbnail::getImage).orElse(null);
            if (image == null) {
                image = ofNullable(refreshThumbnail(transportIdent, imageInfo.getUrl(), this.thumbnailSize))
                        .map(RenderedThumbnail::getImage)
                        .orElse(null);
            }
            return image;
        }

        return null;
    }
}
