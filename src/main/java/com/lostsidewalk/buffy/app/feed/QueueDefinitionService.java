package com.lostsidewalk.buffy.app.feed;

import com.google.gson.Gson;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.request.ExportConfigRequest;
import com.lostsidewalk.buffy.app.model.request.QueueConfigRequest;
import com.lostsidewalk.buffy.app.model.request.FeedStatusUpdateRequest;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import com.lostsidewalk.buffy.queue.QueueDefinition.QueueStatus;
import com.lostsidewalk.buffy.queue.QueueDefinitionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import static com.lostsidewalk.buffy.app.utils.WordUtils.randomWords;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
public class QueueDefinitionService {

    @Autowired
    QueueDefinitionDao queueDefinitionDao;

    public QueueDefinition findByQueueId(String username, Long id) throws DataAccessException {
        return queueDefinitionDao.findByQueueId(username, id);
    }

    public List<QueueDefinition> findByUser(String username) throws DataAccessException {
        List<QueueDefinition> list = queueDefinitionDao.findByUser(username);
        if (list != null) {
            return list;
        }
        return emptyList();
    }

    public Long createFeed(String username, QueueConfigRequest queueConfigRequest) throws DataAccessException, DataUpdateException {
        QueueDefinition newQueueDefinition = QueueDefinition.from(
                queueConfigRequest.getIdent(),
                queueConfigRequest.getTitle(),
                queueConfigRequest.getDescription(),
                queueConfigRequest.getGenerator(),
                getNewTransportIdent().toString(),
                username,
                serializeExportConfig(queueConfigRequest),
                queueConfigRequest.getCopyright(),
                getLanguage(queueConfigRequest.getLanguage()),
                queueConfigRequest.getImgSrc(),
                false
            );
        return queueDefinitionDao.add(newQueueDefinition);
    }

    @SuppressWarnings("UnusedReturnValue") // there are no subsidiary entities to fetch using this Id, thus ignored
    public Long createDefaultFeed(String username) throws DataAccessException, DataUpdateException {
        return createFeed(username, QueueConfigRequest.from(
                generateRandomQueueIdent(),
                "My Queue",
                String.format("Default queue for %s", username),
                "FeedGears 0.4",
                null,
                null,
                null,
                null,
                null
        ));
    }

    private static String generateRandomQueueIdent() {
        return randomWords();
    }

    private Serializable getNewTransportIdent() {
        return UUID.randomUUID().toString();
    }

    public void updateFeed(String username, Long id, QueueConfigRequest queueConfigRequest) throws DataAccessException, DataUpdateException {
        queueDefinitionDao.updateFeed(username, id,
                queueConfigRequest.getIdent(),
                queueConfigRequest.getDescription(),
                queueConfigRequest.getTitle(),
                queueConfigRequest.getGenerator(),
                serializeExportConfig(queueConfigRequest),
                queueConfigRequest.getCopyright(),
                getLanguage(queueConfigRequest.getLanguage()),
                queueConfigRequest.getImgSrc(),
                false
            );
    }

    public void updateFeedStatus(String username, Long id, FeedStatusUpdateRequest feedStatusUpdateRequest) throws DataAccessException, DataUpdateException {
        QueueStatus newStatus = null;
        if (isNotBlank(feedStatusUpdateRequest.getNewStatus())) {
            newStatus = QueueStatus.valueOf(feedStatusUpdateRequest.getNewStatus());
        }
        //
        // perform the update
        //
        queueDefinitionDao.updateQueueStatus(username, id, newStatus);
    }

    private String getLanguage(String lang) {
        return "en-US";
    }

    private static final Gson GSON = new Gson();

    private Serializable serializeExportConfig(QueueConfigRequest queueConfigRequest) {
        ExportConfigRequest e = queueConfigRequest.getExportConfig();
        return e == null ? null : GSON.toJson(e);
    }

    public void deleteById(String username, Long id) throws DataAccessException, DataUpdateException {
        // delete this feed
        queueDefinitionDao.deleteById(username, id);
    }
}
