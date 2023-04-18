package com.lostsidewalk.buffy.app.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
public class CacheConfig {

    // short-lived caches

    @CacheEvict(allEntries = true, value = {"feedDiscoveryCache"})
    @Scheduled(fixedDelay=10_000, initialDelay=480_000)
    public void clearFeedDiscoveryCache() {
        log.trace("Feed discovery cache cleared");
    }

    @CacheEvict(allEntries = true, value = {"feedCollectionDiscoveryCache"})
    @Scheduled(fixedDelay=10_800_000, initialDelay=480_000)
    public void clearFeedCollectionDiscoveryCache() {
        log.trace("Feed collection discovery cache cleared");
    }

    @CacheEvict(allEntries = true, value = {"feedRecommendationCache"})
    @Scheduled(fixedDelay=10_800_000, initialDelay=480_000)
    public void clearFeedRecommendationCache() {
        log.trace("Feed recommendation cache cleared");
    }

    @CacheEvict(allEntries = true, value = {"proxyCache"})
    @Scheduled(fixedDelay=10_000, initialDelay=480_000)
    public void clearProxyCache() {
        log.trace("Proxy cache cleared");
    }

    @CacheEvict(value = {"thumbnailRefreshCache"})
    @Scheduled(fixedDelay=10_000, initialDelay=480_000)
    public void clearThumbnailRefreshCache() {
        log.trace("Thumbnail refresh cache cleared");
    }

    // long-lived caches

    @CacheEvict(allEntries = true, value = {"thumbnailCache"})
    @Scheduled(fixedDelay=10_800_000, initialDelay=480_000)
    public void clearThumbnailCache() {
        log.trace("Thumbnail cache cleared");
    }

    @CacheEvict(allEntries = true, value = {"thumbnailedCatalogCache"})
    @Scheduled(fixedDelay=10_800_000, initialDelay=480_000)
    public void clearThumbnailedCatalogCache() {
        log.trace("Thumbnailed catalog cache cleared");
    }
}
