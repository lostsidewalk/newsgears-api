package com.lostsidewalk.buffy.app.discovery;

import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.FeedDiscoveryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.discoverUrl;

@Slf4j
@Service
public class FeedDiscoveryService {
    //
    // MANUAL DISCOVERY
    //
    @Cacheable(value="feedDiscoveryCache", key="#url")
    public FeedDiscoveryInfo performDiscovery(String url) throws FeedDiscoveryException {
        return discoverUrl(url);
    }
}
