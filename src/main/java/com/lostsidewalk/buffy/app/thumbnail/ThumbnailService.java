package com.lostsidewalk.buffy.app.thumbnail;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.ThumbnailDao;
import com.lostsidewalk.buffy.model.Thumbnail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
public class ThumbnailService {

    @Autowired
    ThumbnailDao thumbnailDao; // for redis interaction

    public Thumbnail getThumbnail(String transportIdent) throws DataAccessException {
        if (isBlank(transportIdent)) {
            return null;
        }
        log.debug("Attempting to locate thumbnail at transportIdent={}", transportIdent);
        Thumbnail thumbnail = thumbnailDao.findThumbnailByTransportIdent(transportIdent);
        if (thumbnail != null) {
            log.debug("Thumbnail located at transportIdent={}", transportIdent);
        } else {
            log.debug("Unable to locate thumbnail at transportIdent={}", transportIdent);
        }

        return thumbnail;
    }

    public Thumbnail refreshThumbnail(String transportIdent, String imgUrl, int targetSize) {
        log.debug("Refreshing thumbnail cache, imgUrl={} @ transportIdent={}", imgUrl, transportIdent);
        Thumbnail thumbnail = null;
        try {
            URL url = new URL(imgUrl);
            byte[] imageBytes = ThumbnailUtils.getImage(url.getPath(), url.openStream(), targetSize);
            thumbnail = Thumbnail.from(transportIdent, imageBytes);
            thumbnailDao.putThumbnailAtTransportIdent(transportIdent, thumbnail);
            log.info("Thumbnail cache updated for imgUrl={} @ transportIdent={}", imgUrl, transportIdent);
        } catch (Exception e) {
            log.warn("Failed to update thumbnail cache for imgUrl={} @ transportIdent={} due to: {}",
                    imgUrl, transportIdent, e.getMessage());
        }

        return thumbnail;
    }

    public Thumbnail refreshThumbnailFromSrc(String transportIdent, String feedImgSrc) throws DataAccessException {
        byte[] imageBytes = ThumbnailUtils.getImage(feedImgSrc);
        Thumbnail thumbnail = Thumbnail.from(transportIdent, imageBytes);
        thumbnailDao.putThumbnailAtTransportIdent(transportIdent, thumbnail);

        return thumbnail;
    }
}
