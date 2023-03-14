package com.lostsidewalk.buffy.app.resolution;

import com.google.gson.Gson;
import com.lostsidewalk.buffy.app.model.request.RssAtomUrl;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryImageInfo;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.FeedDiscoveryException;
import com.lostsidewalk.buffy.post.ContentObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.FeedDiscoveryExceptionType.PARSING_FEED_EXCEPTION;
import static com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.discoverUrl;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.commons.lang3.StringUtils.stripEnd;

@Slf4j
@Service
public class FeedResolutionService {

    private static final Gson GSON = new Gson();

    @Value("${newsgears.userAgent}")
    String feedGearsUserAgent;

    @Value("${feedfinder.urlTemplate}")
    String feedFinderUrlTemplate;

    @Cacheable(value="feedResolutionCache", key="#url")
    public String performResolution(String url) {
        try {
            return resolveUrl(url);
        } catch (IOException e) {
            log.info("FeedFinder resolution failed due to: {}", e.getMessage()); // non-critical
        }

        return null;
    }

    public void resolveIfNecessary(RssAtomUrl rssAtomUrl) throws IOException {
        FeedDiscoveryInfo discoveryInfo = null;
        String feedUrl = rssAtomUrl.getFeedUrl();
        try {
            discoveryInfo = discoverUrl(feedUrl, EMPTY);
        } catch (FeedDiscoveryException e) {
            if (e.exceptionType == PARSING_FEED_EXCEPTION) {
                String resolvedUrl = resolveUrl(feedUrl);
                if (isNotBlank(resolvedUrl) && !StringUtils.equals(resolvedUrl, feedUrl)) {
                    rssAtomUrl.setFeedUrl(resolvedUrl);
                    discoveryInfo = discoverFeed(resolvedUrl);
                }
            }
        }
        if (discoveryInfo != null) {
            rssAtomUrl.setFeedTitle(getFeedTitle(discoveryInfo));
            rssAtomUrl.setFeedImageUrl(getFeedImageUrl(discoveryInfo));
        }
    }

    private FeedDiscoveryInfo discoverFeed(String url) {
        try {
            return discoverUrl(url, feedGearsUserAgent);
        } catch (Exception e) {
            log.debug("Unable to perform feed discovery due to: {}", e.getMessage());
        }

        return null;
    }

    private String getFeedTitle(FeedDiscoveryInfo feedDiscoveryInfo) {
        if (feedDiscoveryInfo != null) {
            ContentObject titleObj = feedDiscoveryInfo.getTitle();
            return titleObj.getValue(); // TODO: might be worth paying attention to 'type', and constructing the title accordingly
        }

        return null;
    }

    private String getFeedImageUrl(FeedDiscoveryInfo feedDiscoveryInfo) {
        if (feedDiscoveryInfo != null) {
            FeedDiscoveryImageInfo imageInfo = feedDiscoveryInfo.getImage();
            if (imageInfo != null) {
                return stripEnd(imageInfo.getUrl(), "/");
            }
            FeedDiscoveryImageInfo iconInfo = feedDiscoveryInfo.getIcon();
            if (iconInfo != null) {
                return stripEnd(iconInfo.getUrl(), "/");
            }
        }

        return EMPTY;
    }


    @Data
    static class FeedFinderResponse {
        Integer code;
        String message;
        List<FeedFinderResult> result;
    }

    @Data
    static class FeedFinderResult {
        String name;
        String description;
        String url;
    }

    private String resolveUrl(String url) throws IOException {
        String feedFinderUrl = String.format(this.feedFinderUrlTemplate, encode(url, UTF_8));
        HttpURLConnection urlConnection = openConnection(feedFinderUrl);
        BufferedReader br;
        if (200 <= urlConnection.getResponseCode() && urlConnection.getResponseCode() <= 299) {
            br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        } else {
            br = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
        }
        String responseBody = br.lines().collect(joining());
        FeedFinderResponse ffResponse = GSON.fromJson(responseBody, FeedFinderResponse.class);
        if (ffResponse != null) {
            if (isNotEmpty(ffResponse.result)) {
                FeedFinderResult finalResult = ffResponse.result.get(0);
                log.info("FeedFinder resolution result, originalUrl={}, name={}, description={}, url={}", url, finalResult.name, finalResult.description, finalResult.url);
                return finalResult.url;
            }
        }

        return null;
    }

    private static HttpURLConnection openConnection(String url) throws IOException {
        URL feedUrl = new URL(url);
        return (HttpURLConnection) feedUrl.openConnection();
    }
}
