package com.lostsidewalk.buffy.app.post;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.request.PostCreateRequest;
import com.lostsidewalk.buffy.app.model.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.PostUpdateRequest;
import com.lostsidewalk.buffy.post.StagingPost;
import com.lostsidewalk.buffy.post.StagingPost.PostStatus;
import com.lostsidewalk.buffy.post.StagingPostDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
public class StagingPostService {

    @Autowired
    StagingPostDao stagingPostDao;

    public List<StagingPost> getStagingPosts(String username, List<Long> feedIds) throws DataAccessException {
        List<StagingPost> list;
        if (isEmpty(feedIds)) {
            list = stagingPostDao.findByUser(username);
        } else {
            list = stagingPostDao.findByUserAndFeedIds(username, feedIds);
        }
        if (list != null) {
            return list;
        }
        return emptyList();
    }

    public void updatePost(String username, long id, PostUpdateRequest postUpdateRequest) throws DataUpdateException, DataAccessException {
        stagingPostDao.updatePost(username, id,
                postUpdateRequest.getSourceName(),
                postUpdateRequest.getSourceUrl(),
                postUpdateRequest.getPostTitle(),
                postUpdateRequest.getPostDesc(),
                postUpdateRequest.getPostUrl(),
                postUpdateRequest.getPostImgUrl(),
                postUpdateRequest.getPostComment(),
                postUpdateRequest.getPostRights(),
                postUpdateRequest.getXmlBase(),
                postUpdateRequest.getContributorName(),
                postUpdateRequest.getContributorEmail(),
                postUpdateRequest.getAuthorName(),
                postUpdateRequest.getAuthorEmail(),
                postUpdateRequest.getPostCategory(),
                postUpdateRequest.getExpirationTimestamp(),
                postUpdateRequest.getEnclosureUrl()
        );
    }

    public void updatePost(String username, Long id, PostStatusUpdateRequest postStatusUpdateRequest) throws DataAccessException, DataUpdateException {
        PostStatus newStatus = null;
        if (isNotBlank(postStatusUpdateRequest.getNewStatus())) {
            newStatus = StagingPost.PostStatus.valueOf(postStatusUpdateRequest.getNewStatus());
        }
        if (newStatus == PostStatus.PUB_PENDING) {
            // (don't update post status if already published)
            if (stagingPostDao.checkPublished(username, id)) {
                return; // return ResponseEntity.badRequest().body(buildResponseMessage("Staging post Id " + id + " is already published."));
            }
        } else if (newStatus == PostStatus.DEPUB_PENDING) {
            // (don't update post status if already published)
            if (!stagingPostDao.checkPublished(username, id)) {
                return; // return ResponseEntity.badRequest().body(buildResponseMessage("Staging post Id " + id + " is not published."));
            }
        } else if (newStatus == PostStatus.IGNORED) {
            // (don't update post status if already published)
            if (stagingPostDao.checkPublished(username, id)) {
                return; // return ResponseEntity.badRequest().body(buildResponseMessage("Staging post Id " + id + " is already published."));
            }
        }
        //
        // perform the update
        //
        stagingPostDao.updatePostStatus(username, id, newStatus);
    }

    public void deleteById(String username, Long id) throws DataAccessException, DataUpdateException {
        if (stagingPostDao.checkPublished(username, id)) {
            return;
        }
        // delete from staging
        stagingPostDao.deleteById(username, id);
    }

    public StagingPost findById(String username, Long id) throws DataAccessException {
        return stagingPostDao.findById(username, id);
    }

    public Long createPost(String username, PostCreateRequest postCreateRequest) throws DataAccessException {
        Date importTimestamp = new Date();
        StagingPost stagingPost = StagingPost.from(
                username, // importer Id (username in this case)
                postCreateRequest.getFeedIdent(),
                buildImporterDesc(username), // importer desc
                buildSourceObject(), // source obj
                postCreateRequest.getSourceName(),
                postCreateRequest.getSourceUrl(),
                postCreateRequest.getTitle(),
                postCreateRequest.getDescription(),
                postCreateRequest.getUrl(),
                postCreateRequest.getImgUrl(),
                importTimestamp,
                EMPTY, // post hash
                username,
                postCreateRequest.getPostComment(),
                false, // is published
                postCreateRequest.getPostRights(),
                postCreateRequest.getXmlBase(),
                postCreateRequest.getContributorName(),
                postCreateRequest.getContributorEmail(),
                postCreateRequest.getAuthorName(),
                postCreateRequest.getAuthorEmail(),
                postCreateRequest.getPostCategory(),
                null, // publish timestamp
                postCreateRequest.getExpirationTimestamp(),
                postCreateRequest.getEnclosureUrl(),
                null
            );

        return stagingPostDao.add(stagingPost);
    }

    private String buildImporterDesc(String username) {
        return "Created by username=" + username;
    }

    private static Serializable buildSourceObject() {
        return "{ \"desc\":\"Manual user submission\" }";
    }
}
