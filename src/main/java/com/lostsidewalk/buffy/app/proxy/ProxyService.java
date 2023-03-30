package com.lostsidewalk.buffy.app.proxy;

import com.lostsidewalk.buffy.app.audit.ProxyUrlHashException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import static com.lostsidewalk.buffy.app.auth.HashingUtils.sha256;
import static java.net.URI.create;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.commons.lang3.StringUtils.*;

@Slf4j
@Service
public class ProxyService {

    @Value("${newsgears.userAgent}")
    String feedGearsUserAgent;

    @Value("${newsgears.imageProxyUrlTemplate}")
    String imageProxyUrlTemplate;

    @Cacheable(value="proxyCache", key="#url")
    public byte[] fetch(String url) throws IOException {
        URL u = new URL(url.replace(" ", "+"));
        URLConnection urlConnection = u.openConnection();
        urlConnection.setRequestProperty("User-Agent", this.feedGearsUserAgent);
        urlConnection.setRequestProperty("Accept-Encoding", "gzip");
        try (InputStream is = urlConnection.getInputStream()) {
            InputStream toRead;
            if (containsIgnoreCase(urlConnection.getContentEncoding(), "gzip")) {
                toRead = new GZIPInputStream(is);
            } else {
                toRead = is;
            }
            return toRead.readAllBytes();
        }
    }

    public String rewriteImageUrl(String imgUrl, String baseUrl) {
        if (startsWith(imgUrl, "/") && isNotBlank(baseUrl)) {
            try {
                URI uri = create(baseUrl);
                imgUrl = uri.resolve("/") + imgUrl;
            } catch (Exception ignored) {}
        }
        if (startsWith(imgUrl, "http")) {
            imgUrl = stripEnd(imgUrl, "/");
            String imgToken = encodeBase64URLSafeString(sha256(imgUrl, UTF_8).getBytes()); // SHA-256 + B64 the URL
            return String.format(this.imageProxyUrlTemplate, strip(imgToken, "="), encode(imgUrl, UTF_8));
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
