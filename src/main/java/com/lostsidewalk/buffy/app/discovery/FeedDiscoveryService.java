package com.lostsidewalk.buffy.app.discovery;

import com.lostsidewalk.buffy.app.model.response.FeedDiscoveryImageInfo;
import com.lostsidewalk.buffy.app.model.response.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.app.model.response.FeedDiscoverySampleItem;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndImage;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.Math.min;
import static java.util.stream.Collectors.toList;
import static javax.xml.bind.DatatypeConverter.printHexBinary;
import static org.apache.commons.lang3.SerializationUtils.serialize;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Slf4j
@Service
public class FeedDiscoveryService {

    @Cacheable(value="feedDiscoveryCache", key="#url")
    public FeedDiscoveryInfo performDiscovery(String url) throws FeedDiscoveryException {
        log.debug("Performing feed discovery for URL={}", url);
        try {
            URL feedSource = new URL(url);
            SyndFeedInput input = new SyndFeedInput();
            XmlReader xmlReader = new XmlReader(feedSource);
            SyndFeed feed = input.build(xmlReader);
            return FeedDiscoveryInfo.from(
                    feed.getTitle(),
                    feed.getDescription(),
                    feed.getFeedType(),
                    feed.getAuthor(),
                    feed.getCopyright(),
                    feed.getDocs(),
                    feed.getEncoding(),
                    feed.getGenerator(),
                    buildFeedImage(feed.getImage()),
                    buildFeedImage(feed.getIcon()),
                    feed.getLanguage(),
                    feed.getLink(),
                    feed.getManagingEditor(),
                    feed.getPublishedDate(),
                    feed.getStyleSheet(),
                    feed.getSupportedFeedTypes(),
                    feed.getWebMaster(),
                    feed.getUri(),
                    firstFive(feed.getEntries())
                            .map(e -> FeedDiscoverySampleItem.from(e.getTitle(), e.getUri(), e.getLink(), e.getUpdatedDate()))
                            .collect(toList())
            );
        } catch (Exception e) {
            throw new FeedDiscoveryException(e.getMessage());
        }
    }

    private FeedDiscoveryImageInfo buildFeedImage(SyndImage img) {
        if (img == null) {
            return null;
        }
        String url = img.getUrl();
        if (isNotBlank(url)) {
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ignored) {
                // ignored
            }
            String transportIdent = computeThumbnailHash(md, url);

            return FeedDiscoveryImageInfo.from(
                    img.getTitle(),
                    img.getDescription(),
                    img.getHeight(),
                    img.getWidth(),
                    img.getLink(),
                    transportIdent,
                    img.getUrl());
        }

        return null;
    }

    private static String computeThumbnailHash(MessageDigest md, String feedImgUrl) {
        return isNotEmpty(feedImgUrl) ? printHexBinary(md.digest(serialize(feedImgUrl))) : null;
    }

    private static Stream<SyndEntry> firstFive(List<SyndEntry> l) {
        return l == null ? Stream.of() : l.subList(0, min(l.size(), 5)).stream();
    }
}
