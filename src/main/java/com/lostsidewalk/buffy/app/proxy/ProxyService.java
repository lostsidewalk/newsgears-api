package com.lostsidewalk.buffy.app.proxy;

import com.lostsidewalk.buffy.app.audit.ProxyUrlHashException;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryImageInfo;
import com.lostsidewalk.buffy.post.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static com.lostsidewalk.buffy.app.auth.HashingUtils.sha256;
import static java.net.URI.create;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.*;
import static org.jsoup.safety.Safelist.relaxed;

@Slf4j
@Service
public class ProxyService {

    @Value("${newsgears.userAgent}")
    String feedGearsUserAgent;

    @Value("${newsgears.imageProxyUrlTemplate}")
    String imageProxyUrlTemplate;

    @Cacheable(value="proxyCache", key="#url")
    public byte[] fetch(String url) throws IOException {
        return fetch(url, 0);
    }

    @Cacheable(value="proxyCache", key="#url")
    public byte[] fetch(String url, int depth) throws IOException {
        //
        HttpURLConnection urlConnection = openConnection(url.replace(" ", "+"));
        //
        int statusCode = urlConnection.getResponseCode();
        //
        String statusMessage = urlConnection.getResponseMessage();
        //
        // (check to broken redirect setups, e.g. http://www.virtualr.net/feed)
        if (isRedirect(statusCode)) {
            if (depth > 2) {
                log.warn("Image proxy fetch exceeded recursion depth (2), url={}, statusCode={}, statusMessage={}", url, statusCode, statusMessage);
                return null;
            }
            // get the redirect location URL
            String redirectUrl = urlConnection.getHeaderField("Location");
            if (isNotBlank(redirectUrl)) {
                return fetch(redirectUrl, depth + 1);
            } else {
                log.warn("Image proxy fetch redirect location is blank, url={}, statusCode={}, statusMessage={}", url, statusCode, statusMessage);
                return null;
            }
        } else if (!isSuccess(statusCode)) {
            log.error("Image proxy fetch failed, url={}, statusCode={}, statusMessage={}", url, statusCode, statusMessage);
            return null;
        }

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

    private HttpURLConnection openConnection(String url) throws IOException {
        URL recommenderUrl = new URL(url);
        HttpURLConnection urlConnection = (HttpURLConnection) recommenderUrl.openConnection();
        urlConnection.setRequestProperty("User-Agent", this.feedGearsUserAgent);
        urlConnection.setRequestProperty("Accept-Encoding", "gzip");
        urlConnection.setDoOutput(true);
        urlConnection.setReadTimeout(5 * 1_000);
        return urlConnection;
    }

    private static boolean isRedirect(int statusCode) {
        return (isTermporaryRedirect(statusCode) || isPermanentRedirect(statusCode)
                || statusCode == HttpURLConnection.HTTP_SEE_OTHER);
    }

    private static boolean isTermporaryRedirect(int statusCode) {
        return statusCode == HttpURLConnection.HTTP_MOVED_TEMP;
    }

    private static boolean isPermanentRedirect(int statusCode) {
        return statusCode == HttpURLConnection.HTTP_MOVED_PERM;
    }

    private static boolean isSuccess(int statusCode) {
        return statusCode == HttpURLConnection.HTTP_OK;
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

    //

    public List<StagingPost> secureStagingPosts(List<StagingPost> stagingPosts) {
        if (isNotEmpty(stagingPosts)) {
            for (StagingPost stagingPost : stagingPosts) {
                String postUrl = stagingPost.getPostUrl();
                // secure the post title HTML content
                secureHtmlContent(stagingPost.getPostTitle(), postUrl);
                // secure the post description HTML content
                secureHtmlContent(stagingPost.getPostDesc(), postUrl);
                // secure the post contents HTML content
                List<ContentObject> postContents = stagingPost.getPostContents();
                if (isNotEmpty(postContents)) {
                    for (ContentObject c : postContents) {
                        secureHtmlContent(c, postUrl);
                    }
                }
                // secure the post iTunes contents
                securePostITunes(stagingPost.getPostITunes(), postUrl);
                // secure the post enclosures
                List<PostEnclosure> postEnclosures = stagingPost.getEnclosures();
                if (isNotEmpty(postEnclosures)) {
                    for (PostEnclosure e : postEnclosures) {
                        securePostEnclosure(e, postUrl);
                    }
                }
                // secure the post media contents
                securePostMedia(stagingPost.getPostMedia(), postUrl);
            }
        }
        return stagingPosts;
    }

    private void secureHtmlContent(ContentObject obj, String baseUrl) {
        if (isHtmlContent(obj)) {
            String rawHtml = obj.getValue();
            String cleanHtml = Jsoup.clean(rawHtml, relaxed()); // this must remove embed and object tags
            Document document = Jsoup.parse(cleanHtml);
            document.getElementsByTag("img").forEach(e -> {
                String imgUrl = e.attr("src");
                e.attr("src", this.rewriteImageUrl(imgUrl, baseUrl));
            });
            document.getElementsByTag("a").forEach(e -> {
                e.attr("target", "_blank");
                e.attr("rel", "noopener");
            });

            //
            obj.setValue(document.toString());
        }
    }

    private static boolean isHtmlContent(ContentObject obj) {
        return obj != null && containsIgnoreCase(obj.getType(), "html");
    }

    private void securePostITunes(PostITunes postITunes, String basesUrl) {
        if (postITunes != null && postITunes.getImageUri() != null) {
            postITunes.setImageUri(this.rewriteImageUrl(postITunes.getImageUri(), basesUrl));
        }
    }

    private void securePostEnclosure(PostEnclosure postEnclosure, String baseUrl) {
        if (isImageEnclosure(postEnclosure)) {
            postEnclosure.setUrl(this.rewriteImageUrl(postEnclosure.getUrl(), baseUrl));
        }
    }

    private static boolean isImageEnclosure(PostEnclosure enc) {
        return enc != null && containsIgnoreCase(enc.getType(), "image");
    }

    private void securePostMedia(PostMedia postMedia, String baseUrl) {
        if (postMedia != null) {
            List<PostMediaContent> postMediaContents = postMedia.getPostMediaContents();
            if (isNotEmpty(postMediaContents)) {
                for (PostMediaContent c : postMediaContents) {
                    securePostMediaContent(c, baseUrl);
                }
            }
            PostMediaMetadata postMediaMetadata = postMedia.getPostMediaMetadata();
            if (postMediaMetadata != null) {
                securePostMediaMetadata(postMediaMetadata, baseUrl);
            }
            List<PostMediaGroup> postMediaGroups = postMedia.getPostMediaGroups();
            for (PostMediaGroup g : postMediaGroups) {
                securePostMediaMetadata(g.getPostMediaMetadata(), baseUrl);
                for (PostMediaContent gc : g.getPostMediaContents()) {
                    securePostMediaContent(gc, baseUrl);
                }
            }
        }
    }

    private void securePostMediaContent(PostMediaContent content, String baseUrl) {
        if (isImageContent(content)) {
            securePostMediaReference(content.getReference(), baseUrl);
        }
    }

    private static boolean isImageContent(PostMediaContent con) {
        return con != null &&
                (containsIgnoreCase(con.getType(), "image") || containsIgnoreCase(con.getMedium(), "image"));
    }

    private void securePostMediaReference(PostMediaReference reference, String baseUrl) {
        if (reference != null) {
            reference.setUri(create(this.rewriteImageUrl(reference.getUri().toString(), baseUrl)));
        }
    }

    private void securePostMediaMetadata(PostMediaMetadata metadata, String baseUrl) {
        if (metadata != null) {
            List<PostMediaThumbnail> postMediaThumbnails = metadata.getThumbnails();
            if (isNotEmpty(postMediaThumbnails)) {
                for (PostMediaThumbnail t : postMediaThumbnails) {
                    securePostMediaThumbnail(t, baseUrl);
                }
            }
        }
    }

    private void securePostMediaThumbnail(PostMediaThumbnail thumbnail, String baseUrl) {
        if (thumbnail != null) {
            thumbnail.setUrl(create(this.rewriteImageUrl(thumbnail.getUrl().toString(), baseUrl)));
        }
    }

    //

    public void secureFeedDiscoveryImageInfo(FeedDiscoveryImageInfo feedDiscoveryImageInfo) {
        if (feedDiscoveryImageInfo != null) {
            String originalUrl = feedDiscoveryImageInfo.getUrl();
            String newUrl = this.rewriteImageUrl(originalUrl, null);

            feedDiscoveryImageInfo.setUrl(newUrl);
        }
    }
}
