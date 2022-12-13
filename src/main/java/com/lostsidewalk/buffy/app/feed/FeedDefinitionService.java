package com.lostsidewalk.buffy.app.feed;

import com.google.gson.Gson;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.request.ExportConfigRequest;
import com.lostsidewalk.buffy.app.model.request.FeedConfigRequest;
import com.lostsidewalk.buffy.app.model.response.FeedToggleResponse;
import com.lostsidewalk.buffy.feed.FeedDefinition;
import com.lostsidewalk.buffy.feed.FeedDefinitionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;

@Service
public class FeedDefinitionService {

    @Autowired
    FeedDefinitionDao feedDefinitionDao;

    public FeedDefinition findByFeedIdent(String username, String feedIdent) throws DataAccessException {
        return feedDefinitionDao.findByFeedIdent(username, feedIdent);
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

    public void create(String username, FeedConfigRequest feedConfigRequest) throws DataAccessException {
        FeedDefinition newFeedDefinition = FeedDefinition.from(
                feedConfigRequest.getIdent(),
                feedConfigRequest.getTitle(),
                feedConfigRequest.getDescription(),
                feedConfigRequest.getGenerator(),
                getNewTransportIdent().toString(),
                username,
                false,
                serializeExportConfig(feedConfigRequest),
                feedConfigRequest.getCopyright(),
                getLanguage(feedConfigRequest.getLanguage()),
                feedConfigRequest.getImgSrc()
            );
        feedDefinitionDao.add(newFeedDefinition);
    }

    private Serializable getNewTransportIdent() {
        return UUID.randomUUID().toString();
    }

    public void update(String username, FeedConfigRequest feedConfigRequest) throws DataAccessException, DataUpdateException {
        feedDefinitionDao.updateFeed(username,
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

    private String getLanguage(String lang) {
        return "en-US";
    }

    private static final Gson GSON = new Gson();

    private Serializable serializeExportConfig(FeedConfigRequest feedConfigRequest) {
        ExportConfigRequest e = feedConfigRequest.getExportConfig();
        return e == null ? null : GSON.toJson(e);
    }

    public FeedToggleResponse toggleActiveById(String username, Long id) throws DataAccessException, DataUpdateException {
        //
        // toggle active
        //
        boolean currentState = feedDefinitionDao.toggleActiveById(username, id);

        FeedToggleResponse feedToggleResponse = new FeedToggleResponse();
        feedToggleResponse.setId(id);
        feedToggleResponse.setActive(currentState);
        feedToggleResponse.setMessage("Feed Id " + id + " is now " + (currentState ? "active" : "inactive"));

        return feedToggleResponse;
    }

    public void deleteById(String username, Long id) throws DataAccessException, DataUpdateException {
        feedDefinitionDao.deleteById(username, id);
    }

    public void markActive(String username, FeedDefinition feedDefinition) throws DataAccessException, DataUpdateException {
        feedDefinitionDao.markActiveById(username, feedDefinition.getId());
    }
}
