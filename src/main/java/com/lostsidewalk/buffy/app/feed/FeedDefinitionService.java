package com.lostsidewalk.buffy.app.feed;

import com.google.gson.Gson;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.request.ExportConfigRequest;
import com.lostsidewalk.buffy.app.model.request.FeedConfigRequest;
import com.lostsidewalk.buffy.app.model.request.FeedStatusUpdateRequest;
import com.lostsidewalk.buffy.feed.FeedDefinition;
import com.lostsidewalk.buffy.feed.FeedDefinition.FeedStatus;
import com.lostsidewalk.buffy.feed.FeedDefinitionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import static com.lostsidewalk.buffy.app.utils.WordUtils.randomWords;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
public class FeedDefinitionService {

    @Autowired
    FeedDefinitionDao feedDefinitionDao;

    public FeedDefinition findByFeedId(String username, Long id) throws DataAccessException {
        return feedDefinitionDao.findByFeedId(username, id);
    }

    public List<String> findIdentsByUser(String username) throws DataAccessException {
        List<String> list = feedDefinitionDao.findIdentsByUser(username);
        if (list != null) {
            return list;
        }
        return emptyList();
    }

    public List<FeedDefinition> findByUser(String username) throws DataAccessException {
        List<FeedDefinition> list = feedDefinitionDao.findByUser(username);
        if (list != null) {
            return list;
        }
        return emptyList();
    }

    public Long createFeed(String username, FeedConfigRequest feedConfigRequest) throws DataAccessException, DataUpdateException {
        FeedDefinition newFeedDefinition = FeedDefinition.from(
                feedConfigRequest.getIdent(),
                feedConfigRequest.getTitle(),
                feedConfigRequest.getDescription(),
                feedConfigRequest.getGenerator(),
                getNewTransportIdent().toString(),
                username,
                serializeExportConfig(feedConfigRequest),
                feedConfigRequest.getCopyright(),
                getLanguage(feedConfigRequest.getLanguage()),
                feedConfigRequest.getImgSrc()
            );
        return feedDefinitionDao.add(newFeedDefinition);
    }

    @SuppressWarnings("UnusedReturnValue") // there are no subsidiary entities to fetch using this Id, thus ignored
    public Long createDefaultFeed(String username) throws DataAccessException, DataUpdateException {
        return createFeed(username, FeedConfigRequest.from(
                generateRandomFeedIdent(),
                "My Queue",
                String.format("Default queue for %s", username),
                "FeedGears 0.4",
                null,
                null,
                null,
                null,
                null,
                null
        ));
    }

    private static String generateRandomFeedIdent() {
        return randomWords();
    }

    private Serializable getNewTransportIdent() {
        return UUID.randomUUID().toString();
    }

    public void update(String username, Long id, FeedConfigRequest feedConfigRequest) throws DataAccessException, DataUpdateException {
        feedDefinitionDao.updateFeed(username, id,
                feedConfigRequest.getIdent(),
                feedConfigRequest.getDescription(),
                feedConfigRequest.getTitle(),
                feedConfigRequest.getGenerator(),
                serializeExportConfig(feedConfigRequest),
                feedConfigRequest.getCopyright(),
                getLanguage(feedConfigRequest.getLanguage()),
                feedConfigRequest.getImgSrc()
            );
    }

    public void update(String username, Long id, FeedStatusUpdateRequest feedStatusUpdateRequest) throws DataAccessException, DataUpdateException {
        FeedStatus newStatus = null;
        if (isNotBlank(feedStatusUpdateRequest.getNewStatus())) {
            newStatus = FeedStatus.valueOf(feedStatusUpdateRequest.getNewStatus());
        }
        //
        // perform the update
        //
        feedDefinitionDao.updateFeedStatus(username, id, newStatus);
    }

    private String getLanguage(String lang) {
        return "en-US";
    }

    private static final Gson GSON = new Gson();

    private Serializable serializeExportConfig(FeedConfigRequest feedConfigRequest) {
        ExportConfigRequest e = feedConfigRequest.getExportConfig();
        return e == null ? null : GSON.toJson(e);
    }

    public void deleteById(String username, Long id) throws DataAccessException, DataUpdateException {
        // delete this feed
        feedDefinitionDao.deleteById(username, id);
    }
}
