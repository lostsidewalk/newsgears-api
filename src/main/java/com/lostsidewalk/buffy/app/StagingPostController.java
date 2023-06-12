package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.response.DeployResponse;
import com.lostsidewalk.buffy.app.model.response.PostFetchResponse;
import com.lostsidewalk.buffy.app.model.response.ThumbnailedPostResponse;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.app.proxy.ProxyService;
import com.lostsidewalk.buffy.post.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.lostsidewalk.buffy.app.ResponseMessageUtils.buildResponseMessage;
import static com.lostsidewalk.buffy.app.user.UserRoles.UNVERIFIED_ROLE;
import static org.apache.commons.collections4.CollectionUtils.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@Validated
public class StagingPostController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    ProxyService proxyService;
    //
    // get staging posts
    //
    @GetMapping("/staging")
    @Secured({UNVERIFIED_ROLE})
    public ResponseEntity<PostFetchResponse> getStagingPosts(@RequestParam(required = false) List<Long> queueIds, Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
            log.debug("getStagingPosts for user={}, queueIds={}", username, isEmpty(queueIds) ? "all" : queueIds);
        StopWatch stopWatch = StopWatch.createStarted();
        List<ThumbnailedPostResponse> stagingPosts =
                addThumbnails(
                        proxyService.secureStagingPosts(
                                stagingPostService.getStagingPosts(username, queueIds)));
        stopWatch.stop();
        appLogService.logStagingPostFetch(username, stopWatch, size(queueIds), size(stagingPosts));
        return ok(PostFetchResponse.from(stagingPosts));
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
    @PutMapping("/staging/read-status/queue/{id}")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> updateFeedReadStatus(@PathVariable Long id, @Valid @RequestBody PostStatusUpdateRequest postStatusUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostStatus for user={}, postId={}, postStatusUpdateRequest={}", username, id, postStatusUpdateRequest);
        StopWatch stopWatch = StopWatch.createStarted();
        stagingPostService.updateQueueReadStatus(username, id, postStatusUpdateRequest);
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

    private List<ThumbnailedPostResponse> addThumbnails(List<StagingPost> stagingPosts) {
        List<ThumbnailedPostResponse> responses = newArrayListWithCapacity(size(stagingPosts));
        for (StagingPost stagingPost : stagingPosts) {
            responses.add(addThumbnail(stagingPost));
        }
        return responses;
    }

    private ThumbnailedPostResponse addThumbnail(StagingPost s) {
        String imageProxyUrl = buildThumbnailProxyUrl(s);
        return ThumbnailedPostResponse.from(s, imageProxyUrl);
    }

    private String buildThumbnailProxyUrl(StagingPost s) {
        if (isNotBlank(s.getPostImgUrl())) {
            return proxyService.rewriteImageUrl(s.getPostImgUrl(), s.getPostUrl());
        }

        return null;
    }
}
