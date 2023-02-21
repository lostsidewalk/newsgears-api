package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.Publisher.PubResult;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.PostUpdateRequest;
import com.lostsidewalk.buffy.app.model.response.DeployResponse;
import com.lostsidewalk.buffy.app.model.response.PostConfigResponse;
import com.lostsidewalk.buffy.app.model.response.PostFetchResponse;
import com.lostsidewalk.buffy.app.model.response.ThumbnailedPostResponse;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.app.proxy.ProxyService;
import com.lostsidewalk.buffy.app.thumbnail.ThumbnailService;
import com.lostsidewalk.buffy.model.RenderedThumbnail;
import com.lostsidewalk.buffy.post.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.lostsidewalk.buffy.app.ResponseMessageUtils.buildResponseMessage;
import static com.lostsidewalk.buffy.app.user.UserRoles.UNVERIFIED_ROLE;
import static java.net.URI.create;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.CollectionUtils.*;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.jsoup.safety.Safelist.relaxed;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
public class StagingPostController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    ThumbnailService thumbnailService;

    @Autowired
    ProxyService proxyService;

    @Value("${newsgears.thumbnail.size}")
    int thumbnailSize;
    //
    // get staging posts
    //
    @GetMapping("/staging")
    @Secured({UNVERIFIED_ROLE})
    public ResponseEntity<PostFetchResponse> getStagingPosts(@RequestParam(required = false) List<Long> feedIds, Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
            log.debug("getStagingPosts for user={}, feedIds={}", username, isEmpty(feedIds) ? "all" : feedIds);
        StopWatch stopWatch = StopWatch.createStarted();
        List<ThumbnailedPostResponse> stagingPosts = addThumbnails(secureStagingPosts(stagingPostService.getStagingPosts(username, feedIds)));
        stopWatch.stop();
        appLogService.logStagingPostFetch(username, stopWatch, size(feedIds), size(stagingPosts));
        return ok(PostFetchResponse.from(stagingPosts));
    }

//    @PostMapping("/staging")
//    @Secured({UNVERIFIED_ROLE})
//    @Transactional
//    public ResponseEntity<?> createPost(@Valid @RequestBody PostCreateRequest postCreateRequest, Authentication authentication) throws DataAccessException {
//        UserDetails userDetails = (UserDetails) authentication.getDetails();
//        String username = userDetails.getUsername();
//        log.debug("createPost for user={}, postCreateRequest={}", username, postCreateRequest);
//        StopWatch stopWatch = StopWatch.createStarted();
//        Long id = stagingPostService.createPost(username, postCreateRequest);
//        stopWatch.stop();
//        appLogService.logStagingPostCreate(username, stopWatch, id);
//        return ok().body(buildResponseMessage("Successfully added post to feedId=" + postCreateRequest.getFeedId()));
//    }

    @PutMapping("/staging/{id}")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePost(@PathVariable Long id, @Valid @RequestBody PostUpdateRequest postUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePost for user={}, postId={}, postUpdateRequest={}", username, id, postUpdateRequest);
        StopWatch stopWatch = StopWatch.createStarted();
        stagingPostService.updatePost(username, id, postUpdateRequest);
        StagingPost s = stagingPostService.findById(username, id);
        byte[] thumbnail = buildThumbnail(s);
        stopWatch.stop();
        appLogService.logStagingPostUpdate(username, stopWatch);
        return ok(PostConfigResponse.from(thumbnail));
    }

    /**
     * READ -- mark the post as read (applies to non-published posts only)
     * READ_LATER -- mark the post as read-later (applies to non-published posts only)
     * null -- clear the current post-read status (i.e., UNREAD)
     */
    @PutMapping("/staging/read-status/post/{id}")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> updatePostReadStatus(@PathVariable Long id, @Valid @RequestBody PostStatusUpdateRequest postStatusUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostStatus for user={}, postId={}, postStatusUpdateRequest={}", username, id, postStatusUpdateRequest);
        StopWatch stopWatch = StopWatch.createStarted();
        stagingPostService.updatePostReadStatus(username, id, postStatusUpdateRequest);
        stopWatch.stop();
        appLogService.logStagingPostReadStatusUpdate(username, stopWatch, id, postStatusUpdateRequest, 1);
        return ok().body(buildResponseMessage("Successfully updated post Id " + id));
    }

    /**
     * READ -- mark the post as read (applies to non-published posts only)
     * READ_LATER -- mark the post as read-later (applies to non-published posts only)
     * null -- clear the current post-read status (i.e., UNREAD)
     */
    @PutMapping("/staging/read-status/feed/{id}")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> updateFeedReadStatus(@PathVariable Long id, @Valid @RequestBody PostStatusUpdateRequest postStatusUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostStatus for user={}, postId={}, postStatusUpdateRequest={}", username, id, postStatusUpdateRequest);
        StopWatch stopWatch = StopWatch.createStarted();
        stagingPostService.updateFeedReadStatus(username, id, postStatusUpdateRequest);
        stopWatch.stop();
        appLogService.logFeedReadStatusUpdate(username, stopWatch, id, postStatusUpdateRequest, 1);
        return ok().body(buildResponseMessage("Successfully updated feed Id " + id));
    }

    /**
     * PUB_PENDING -- mark the post for publication, will be deployed with the feed
     * DEPUB_PENDING -- mark the post for de-publication, will be unpublished and excluded from future deployments
     * null -- clear the current post-pub status
     */
    @PutMapping("/staging/pub-status/{id}")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> updatePostPubStatus(@PathVariable Long id, @Valid @RequestBody PostStatusUpdateRequest postStatusUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostStatus for user={}, postId={}, postStatusUpdateRequest={}", username, id, postStatusUpdateRequest);
        StopWatch stopWatch = StopWatch.createStarted();
        List<PubResult> publicationResults = stagingPostService.updatePostPubStatus(username, id, postStatusUpdateRequest);
        stopWatch.stop();
        appLogService.logStagingPostPubStatusUpdate(username, stopWatch, id, postStatusUpdateRequest, 1, publicationResults);
        return ok().body(DeployResponse.from(publicationResults));
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
                e.attr("src", proxyService.rewriteImageUrl(imgUrl, baseUrl));
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
            postITunes.setImageUri(proxyService.rewriteImageUrl(postITunes.getImageUri(), basesUrl));
        }
    }

    private void securePostEnclosure(PostEnclosure postEnclosure, String baseUrl) {
        if (isImageEnclosure(postEnclosure)) {
            postEnclosure.setUrl(proxyService.rewriteImageUrl(postEnclosure.getUrl(), baseUrl));
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
        return con != null && containsIgnoreCase(con.getType(), "image");
    }

    private void securePostMediaReference(PostMediaReference reference, String baseUrl) {
        if (reference != null) {
            reference.setUri(create(proxyService.rewriteImageUrl(reference.getUri().toString(), baseUrl)));
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
            thumbnail.setUrl(create(proxyService.rewriteImageUrl(thumbnail.getUrl().toString(), baseUrl)));
        }
    }

    //

    private List<ThumbnailedPostResponse> addThumbnails(List<StagingPost> stagingPosts) throws DataAccessException {
        List<ThumbnailedPostResponse> responses = newArrayListWithCapacity(size(stagingPosts));
        for (StagingPost stagingPost : stagingPosts) {
            responses.add(this.addThumbnail(stagingPost));
        }
        return responses;
    }

    private ThumbnailedPostResponse addThumbnail(StagingPost s) throws DataAccessException {
        byte[] image = buildThumbnail(s);
        return ThumbnailedPostResponse.from(s, image);
    }

    private byte[] buildThumbnail(StagingPost s) throws DataAccessException {
        if (isNotBlank(s.getPostImgUrl())) {
            String transportIdent = s.getPostImgTransportIdent();
            byte[] image = ofNullable(thumbnailService.getThumbnail(transportIdent)).map(RenderedThumbnail::getImage).orElse(null);
            if (image == null) {
                image = ofNullable(thumbnailService.refreshThumbnail(transportIdent, s.getPostImgUrl(), this.thumbnailSize))
                        .map(RenderedThumbnail::getImage)
                        .orElse(null);
            }
            return image;
        }

        return null;
    }
}
