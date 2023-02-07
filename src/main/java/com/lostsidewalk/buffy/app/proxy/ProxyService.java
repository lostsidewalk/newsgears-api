package com.lostsidewalk.buffy.app.proxy;

import com.lostsidewalk.buffy.app.audit.ProxyUrlHashException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import static com.lostsidewalk.buffy.app.auth.HashingUtils.sha256;
import static java.net.URI.create;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.commons.lang3.StringUtils.*;

@Slf4j
@Service
public class ProxyService {


    @Cacheable(value="proxyCache", key="#url")
    public byte[] fetch(String url) throws IOException {
        URL u = new URL(url.replace(" ", "+"));
        URLConnection urlConnection = u.openConnection();
        // TODO: make this property-configurable
        String userAgent = "Lost Sidewalk FeedGears RSS Aggregator v.0.4";
        urlConnection.setRequestProperty("User-Agent", userAgent);
        urlConnection.setRequestProperty("Accept-Encoding", "identity");
        try (InputStream is = urlConnection.getInputStream()) {
            return is.readAllBytes();
        }
    }

    public String rewriteImageUrl(String imgUrl, String baseUrl) {
        if (startsWith(imgUrl, "/")) {
            try {
                URI uri = create(baseUrl);
                imgUrl = uri.resolve("/") + imgUrl;
            } catch (Exception ignored) {}
        }
        if (startsWith(imgUrl, "http")) {
            String imgToken = encodeBase64URLSafeString(sha256(imgUrl, UTF_8).getBytes()); // SHA-256 + B64 the URL
            // TODO: get the base URL from a property
            return String.format("http://localhost:8080/proxy/unsecured/%s/?url=%s", strip(imgToken, "="), encode(imgUrl, UTF_8));
        }

        return EMPTY;
    }

    public void validateImageUrl(String imgUrl, String hash) throws ProxyUrlHashException {
        String targetHash = strip(encodeBase64URLSafeString(sha256(imgUrl, UTF_8).getBytes()), "=");
        if (!StringUtils.equals(targetHash, hash)) {
            throw new ProxyUrlHashException(imgUrl, hash);
        }
    }
}
