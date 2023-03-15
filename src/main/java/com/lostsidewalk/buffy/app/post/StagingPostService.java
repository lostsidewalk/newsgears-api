package com.lostsidewalk.buffy.app.post;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.PostPublisher;
import com.lostsidewalk.buffy.Publisher.PubResult;
import com.lostsidewalk.buffy.app.model.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.post.StagingPost;
import com.lostsidewalk.buffy.post.StagingPost.PostPubStatus;
import com.lostsidewalk.buffy.post.StagingPost.PostReadStatus;
import com.lostsidewalk.buffy.post.StagingPostDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
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

    @SuppressWarnings("UnusedReturnValue")
    public List<PubResult> updateFeedPubStatus(String username, Long id, PostPubStatus newStatus) throws DataAccessException, DataUpdateException {
        //
        // perform the update
        //
        stagingPostDao.updateFeedPubStatus(username, id, newStatus);
        //
        // deploy the feed
        //
        return postPublisher.publishFeed(username, id);
    }
}
