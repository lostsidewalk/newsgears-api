package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.request.PostCreateRequest;
import com.lostsidewalk.buffy.app.model.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.PostUpdateRequest;
import com.lostsidewalk.buffy.app.model.response.PostConfigResponse;
import com.lostsidewalk.buffy.app.model.response.PostFetchResponse;
import com.lostsidewalk.buffy.app.model.response.ThumbnailedPostResponse;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.app.thumbnail.ThumbnailService;
import com.lostsidewalk.buffy.model.Thumbnail;
import com.lostsidewalk.buffy.post.StagingPost;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.lostsidewalk.buffy.app.ResponseMessageUtils.buildResponseMessage;
import static com.lostsidewalk.buffy.app.user.UserRoles.UNVERIFIED_ROLE;
import static com.lostsidewalk.buffy.app.user.UserRoles.VERIFIED_ROLE;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.ResponseEntity.*;

@Slf4j
@RestController
public class StagingPostController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    ThumbnailService thumbnailService;
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
        List<ThumbnailedPostResponse> stagingPosts = addThumbnails(stagingPostService.getStagingPosts(username, feedIds));
        stopWatch.stop();
        appLogService.logStagingPostFetch(username, stopWatch, size(feedIds), size(stagingPosts));
        return ok(PostFetchResponse.from(stagingPosts));
    }

    @PostMapping("/staging")
    @Secured({VERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> createPost(@Valid @RequestBody PostCreateRequest postCreateRequest, Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("createPost for user={}, postCreateRequest={}", username, postCreateRequest);
        StopWatch stopWatch = StopWatch.createStarted();
        Long id = stagingPostService.createPost(username, postCreateRequest);
        stopWatch.stop();
        appLogService.logStagingPostCreate(username, stopWatch, id);
        return ok().body(buildResponseMessage("Successfully added post to feedIdent=" + postCreateRequest.getFeedIdent()));
    }

    @PutMapping("/staging/{id}")
    @Secured({VERIFIED_ROLE})
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
     * PUB_PENDING -- mark the post for publication, will be deployed next
     * DEPUB_PENDING -- mark the post for de-publication, will be excluded from the next deployment and mark is_published = false
     * IGNORED -- mark the post as ignored (applies to staging posts only)
     * null -- clear the current post status
     *  -- clearing the status of currently deployed post means removing the DEPUB_PENDING flag
     *  -- clearing the status of a staging post means removing the PUB_PENDING or IGNORED flags
     */
    @PutMapping("/staging/status/{id}")
    @Secured({VERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> updatePostStatus(@PathVariable Long id, @Valid @RequestBody PostStatusUpdateRequest postStatusUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostStatus for user={}, postId={}, postStatusUpdateRequest={}", username, id, postStatusUpdateRequest);
        StopWatch stopWatch = StopWatch.createStarted();
        stagingPostService.updatePost(username, id, postStatusUpdateRequest);
        stopWatch.stop();
        appLogService.logStagingPostStatusUpdate(username, stopWatch, id, postStatusUpdateRequest, 1);
        return ok().body(buildResponseMessage("Successfully updated post Id " + id));
    }
    //
    // delete staging post
    //
    @DeleteMapping("/staging/{id}")
    @Secured({UNVERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> deleteStagingPost(@PathVariable Long id, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteById for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        stagingPostService.deleteById(username, id);
        stopWatch.stop();
        appLogService.logStagingPostDelete(username, stopWatch, id, 1);
        return ok().body(buildResponseMessage("Deleted staging post Id " + id));
    }

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
            byte[] image = ofNullable(thumbnailService.getThumbnail(transportIdent)).map(Thumbnail::getImage).orElse(null);
            if (image == null) {
                image = ofNullable(thumbnailService.refreshThumbnail(transportIdent, s.getPostImgUrl(), 140))
                        .map(Thumbnail::getImage)
                        .orElse(null);
            }
            return image;
        }

        return null;
    }
}
