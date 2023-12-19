package com.lostsidewalk.buffy.app.discovery;

import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.FeedDiscoveryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static com.lostsidewalk.buffy.rss.RssDiscovery.discoverUrl;

@Slf4j
@Service
public class FeedDiscoveryService {

    @Value("${newsgears.userAgent}")
    String feedGearsUserAgent;
    //
    // INDIVIDUAL URL DISCOVERY
    //
    @Cacheable(value="feedDiscoveryCache")
    public FeedDiscoveryInfo performDiscovery(String url, String username, String password) throws FeedDiscoveryException {
        return discoverUrl(url, username, password, feedGearsUserAgent);
    }
}
