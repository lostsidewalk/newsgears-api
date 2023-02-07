package com.lostsidewalk.buffy.app.post;

import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.PostPublisher;
import com.lostsidewalk.buffy.Publisher.PubResult;
import com.lostsidewalk.buffy.app.model.request.PostCreateRequest;
import com.lostsidewalk.buffy.app.model.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.PostUpdateRequest;
import com.lostsidewalk.buffy.post.StagingPost;
import com.lostsidewalk.buffy.post.StagingPost.PostPubStatus;
import com.lostsidewalk.buffy.post.StagingPost.PostReadStatus;
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

    @Autowired
    PostPublisher postPublisher;

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
                postUpdateRequest.getPostContents(),
                postUpdateRequest.getPostMedia(),
                postUpdateRequest.getPostITunes(),
                postUpdateRequest.getPostUrl(),
                postUpdateRequest.getPostUrls(),
                postUpdateRequest.getPostImgUrl(),
                postUpdateRequest.getPostComment(),
                postUpdateRequest.getPostRights(),
                postUpdateRequest.getContributors(),
                postUpdateRequest.getAuthors(),
                postUpdateRequest.getPostCategories(),
                postUpdateRequest.getExpirationTimestamp(),
                postUpdateRequest.getEnclosures());
    }

    public void updatePostReadStatus(String username, Long id, PostStatusUpdateRequest postStatusUpdateRequest) throws DataAccessException, DataUpdateException {
        PostReadStatus newStatus = null;
        if (isNotBlank(postStatusUpdateRequest.getNewStatus())) {
            newStatus = PostReadStatus.valueOf(postStatusUpdateRequest.getNewStatus());
        }
        //
        // perform the update
        //
        stagingPostDao.updatePostReadStatus(username, id, newStatus);
    }

    public void updateFeedReadStatus(String username, Long id, PostStatusUpdateRequest postStatusUpdateRequest) throws DataAccessException, DataUpdateException {
        PostReadStatus newStatus = null;
        if (isNotBlank(postStatusUpdateRequest.getNewStatus())) {
            newStatus = PostReadStatus.valueOf(postStatusUpdateRequest.getNewStatus());
        }
        //
        // perform the update
        //
        stagingPostDao.updateFeedReadStatus(username, id, newStatus);
    }

    public List<PubResult> updatePostPubStatus(String username, Long id, PostStatusUpdateRequest postStatusUpdateRequest) throws DataAccessException, DataUpdateException {
        PostPubStatus newStatus = null;
        if (isNotBlank(postStatusUpdateRequest.getNewStatus())) {
            newStatus = PostPubStatus.valueOf(postStatusUpdateRequest.getNewStatus());
        }
        //
        // perform the update
        //
        stagingPostDao.updatePostPubStatus(username, id, newStatus);
        //
        // deploy the feed
        //
        Long feedId = stagingPostDao.findFeedIdByStagingPostId(username, id);
        return postPublisher.publishFeed(username, feedId);
    }

    public StagingPost findById(String username, Long id) throws DataAccessException {
        return stagingPostDao.findById(username, id);
    }

    public Long createPost(String username, PostCreateRequest postCreateRequest) throws DataAccessException {
        Date importTimestamp = new Date();
        StagingPost stagingPost = StagingPost.from(
                "FeedGears", // importer Id ("FeedGears") // TODO: make this a property
                postCreateRequest.getFeedId(),
                buildImporterDesc(username), // importer desc (username)
                buildSourceObject(), // source obj
                postCreateRequest.getSourceName(),
                postCreateRequest.getSourceUrl(),
                postCreateRequest.getPostTitle(),
                postCreateRequest.getPostDesc(),
                postCreateRequest.getPostContents(),
                postCreateRequest.getPostMedia(),
                postCreateRequest.getPostITunes(),
                postCreateRequest.getPostUrl(),
                postCreateRequest.getPostUrls(),
                postCreateRequest.getPostImgUrl(),
                importTimestamp,
                EMPTY, // post hash
                username,
                postCreateRequest.getPostComment(),
                postCreateRequest.getPostRights(),
                postCreateRequest.getContributors(),
                postCreateRequest.getAuthors(),
                postCreateRequest.getPostCategories(),
                null, // publish timestamp
                postCreateRequest.getExpirationTimestamp(),
                postCreateRequest.getEnclosures(),
                null
            );

        return stagingPostDao.add(stagingPost);
    }

    private String buildImporterDesc(String username) {
        return username;
    }

    private static Serializable buildSourceObject() {
        JsonObject sourceObject = new JsonObject();
        sourceObject.addProperty("desc", "Manual user submission");
        return sourceObject.toString();
    }
}
