package com.lostsidewalk.buffy.app.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
public class CacheConfig {

    @CacheEvict(allEntries = true, value = {"feedDiscoveryCache"})
    @Scheduled(fixedDelay=10000, initialDelay=10000)
    public void clearCache() {
        log.trace("Caches cleared");
    }
}
