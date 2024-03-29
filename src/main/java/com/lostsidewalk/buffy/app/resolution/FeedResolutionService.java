package com.lostsidewalk.buffy.app.resolution;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.lostsidewalk.buffy.app.model.request.SubscriptionConfigRequest;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryImageInfo;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.FeedDiscoveryException;
import com.lostsidewalk.buffy.post.ContentObject;
import jakarta.annotation.PostConstruct;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static com.google.common.collect.ImmutableMap.copyOf;
import static com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.FeedDiscoveryExceptionType.PARSING_FEED_EXCEPTION;
import static com.lostsidewalk.buffy.rss.RssDiscovery.discoverUrl;
import static java.lang.Math.min;
import static java.lang.Runtime.getRuntime;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.StringUtils.*;

@Slf4j
@Service
public class FeedResolutionService {

    private static final Gson GSON = new Gson();

    @Value("${newsgears.userAgent}")
    String feedGearsUserAgent;

    @Value("${feedfinder.urlTemplate}")
    String feedFinderUrlTemplate;

    private ExecutorService feedResolutionThreadPool;

    @PostConstruct
    public void postConstruct() {
        //
        // setup the feed definition thread pool
        //
        int availableProcessors = getRuntime().availableProcessors();
        int processorCt = availableProcessors > 1 ? min(24, availableProcessors - 1) : availableProcessors;
        processorCt = processorCt >= 2 ? processorCt - 1 : processorCt; // account for the import processor thread
        log.info("Starting feed resolution thread pool: processCount={}", processorCt);
        this.feedResolutionThreadPool = newFixedThreadPool(processorCt, new ThreadFactoryBuilder().setNameFormat("feed-resolution-%d").build());
    }

    @Cacheable(value="feedResolutionCache", key="#url")
    public String performResolution(String url) {
        try {
            return resolveUrl(url);
        } catch (IOException e) {
            log.info("FeedFinder resolution failed due to: {}", e.getMessage()); // non-critical
        }

        return null;
    }

    public ImmutableMap<String, FeedDiscoveryInfo> resolveIfNecessary(List<SubscriptionConfigRequest> subscriptions) {
        CountDownLatch latch = new CountDownLatch(size(subscriptions));
        Map<String, FeedDiscoveryInfo> discoveryCache = new HashMap<>();
        if (isNotEmpty(subscriptions)) {
            for (SubscriptionConfigRequest r : subscriptions) {
                if (isBlank(r.getUrl())) {
                    log.warn("Unable to perform feed URL due to missing URL, skipping...");
                    latch.countDown();
                    continue;
                }
                this.feedResolutionThreadPool.submit(() -> {
                    FeedDiscoveryInfo discoveryInfo;
                    try {
                        discoveryInfo = resolveIfNecessary(r);
                        if (discoveryInfo != null) {
                            discoveryCache.put(r.getUrl(), discoveryInfo);
                        }
                    } catch (IOException e) {
                        log.warn("Feed resolution failed due to: {}", e.getMessage());
                    }
                    latch.countDown();
                });
            }
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("Feed resolution interrupted due to: {}", e.getMessage());
        }

        return copyOf(discoveryCache);
    }

    private FeedDiscoveryInfo resolveIfNecessary(SubscriptionConfigRequest subscription) throws IOException {
        log.info("Performing feed resolution on URL={}", subscription.getUrl());
        FeedDiscoveryInfo discoveryInfo = null;
        String feedUrl = subscription.getUrl();
        try {
            discoveryInfo = discoverUrl(feedUrl, subscription.getUsername(), subscription.getPassword(), feedGearsUserAgent);
        } catch (FeedDiscoveryException e) {
            if (e.exceptionType == PARSING_FEED_EXCEPTION) {
                String resolvedUrl = resolveUrl(feedUrl);
                if (isNotBlank(resolvedUrl) && !StringUtils.equals(resolvedUrl, feedUrl)) {
                    subscription.setUrl(resolvedUrl);
                    try {
                        discoveryInfo = discoverUrl(resolvedUrl, subscription.getUsername(), subscription.getPassword(), feedGearsUserAgent);
                    } catch (Exception e1) {
                        log.warn("Unable to perform feed discovery due to: {}", e1.getMessage());
                    }
                }
            } else {
                log.warn("Feed resolution failed for URL={} due to: {}", subscription.getUrl(), e.getMessage());
            }
        }
        if (discoveryInfo != null) {
            subscription.setTitle(getFeedTitle(discoveryInfo));
            subscription.setImgUrl(getFeedImageUrl(discoveryInfo));
        }

        return discoveryInfo;
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
