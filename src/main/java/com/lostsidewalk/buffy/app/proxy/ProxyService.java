package com.lostsidewalk.buffy.app.proxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

@Slf4j
@Service
public class ProxyService {


    @Cacheable(value="proxyCache", key="#url")
    public byte[] fetch(String url) throws IOException {
        URL u = new URL(url.replace(" ", "+"));
        URLConnection urlConnection = u.openConnection();
        // TODO: make this property-configurable
        String userAgent = "Lost Sidewalk FeedGears RSS Aggregator v.0.3";
        urlConnection.setRequestProperty("User-Agent", userAgent);
        urlConnection.setRequestProperty("Accept-Encoding", "identity");
        return urlConnection.getInputStream().readAllBytes();
    }
}
