package com.lostsidewalk.buffy.app.post;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.post.StagingPost;
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

    public List<StagingPost> getStagingPosts(String username, List<Long> queueIds) throws DataAccessException {
        List<StagingPost> list;
        if (isEmpty(queueIds)) {
            list = stagingPostDao.findByUser(username);
        } else {
            list = stagingPostDao.findByUserAndQueueIds(username, queueIds);
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

    public void updateQueueReadStatus(String username, Long id, PostStatusUpdateRequest postStatusUpdateRequest) throws DataAccessException, DataUpdateException {
        PostReadStatus newStatus = null;
        if (isNotBlank(postStatusUpdateRequest.getNewStatus())) {
            newStatus = PostReadStatus.valueOf(postStatusUpdateRequest.getNewStatus());
        }
        //
        // perform the update
        //
        stagingPostDao.updateQueueReadStatus(username, id, newStatus);
    }
}
