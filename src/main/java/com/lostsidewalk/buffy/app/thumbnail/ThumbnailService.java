package com.lostsidewalk.buffy.app.thumbnail;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.RenderedThumbnailDao;
import com.lostsidewalk.buffy.app.audit.ErrorLogService;
import com.lostsidewalk.buffy.model.RenderedThumbnail;
import com.lostsidewalk.buffy.thumbnail.ThumbnailDao;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.apache.commons.lang3.StringUtils.isBlank;

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
}
