package com.lostsidewalk.buffy.app.query;

import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.request.FeedConfigRequest;
import com.lostsidewalk.buffy.app.model.request.RssAtomUrl;
import com.lostsidewalk.buffy.query.QueryDefinition;
import com.lostsidewalk.buffy.query.QueryDefinitionDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.lostsidewalk.buffy.rss.RssImporter.RSS;
import static java.util.List.copyOf;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
public class QueryDefinitionService {

    @Autowired
    QueryDefinitionDao queryDefinitionDao;

    public List<QueryDefinition> findByUsername(String username) throws DataAccessException {
        return queryDefinitionDao.findByUsername(username);
    }

    public List<QueryDefinition> findByFeedId(String username, Long id) throws DataAccessException {
        return queryDefinitionDao.findByFeedId(username, id);
    }

    public List<QueryDefinition> updateQueries(String username, Long feedId, FeedConfigRequest feedConfigRequest) throws DataAccessException, DataUpdateException {
        List<QueryDefinition> updatedQueries = new ArrayList<>();
        // add/update the RSS/ATOM query URLs
        List<QueryDefinition> rssAtomQueries = configureRssAtomQueries(username, feedId, feedConfigRequest.getRssAtomFeedUrls());
        if (isNotEmpty(rssAtomQueries)) {
            updatedQueries.addAll(rssAtomQueries);
        }

        return updatedQueries;
    }

    public List<QueryDefinition> addQueries(String username, Long feedId, List<RssAtomUrl> rssAtomUrls) throws DataAccessException, DataUpdateException {
        List<QueryDefinition> createdQueries = new ArrayList<>();
        if (isNotEmpty(rssAtomUrls)) {
            createdQueries.addAll(addRssAtomQueries(username, feedId, rssAtomUrls));
        }

        return createdQueries;
    }

    private List<QueryDefinition> addRssAtomQueries(String username, Long feedId, List<RssAtomUrl> rssAtomFeedUrls) throws DataAccessException, DataUpdateException {
        List<QueryDefinition> adds = new ArrayList<>();
        for (RssAtomUrl r : rssAtomFeedUrls) {
            String url = r.getFeedUrl();
            if (isBlank(url)) {
                log.warn("Query is missing a URL, skipping...");
                continue;
            }
            String title = r.getFeedTitle();
            String imageUrl = r.getFeedImageUrl();
            QueryDefinition newQuery = QueryDefinition.from(feedId, username, title, imageUrl, url, RSS, serializeQueryConfig(r));
            adds.add(newQuery);
        }
        List<Long> queryIds = queryDefinitionDao.add(adds);

        return queryDefinitionDao.findByIds(username, queryIds);
    }

    public List<QueryDefinition> createQueries(String username, Long feedId, List<RssAtomUrl> rssAtomUrls) throws DataAccessException, DataUpdateException {
        List<QueryDefinition> createdQueries = new ArrayList<>();
        if (isNotEmpty(rssAtomUrls)) {
            createdQueries.addAll(configureRssAtomQueries(username, feedId, rssAtomUrls));
        }

        return createdQueries;
    }

    private List<QueryDefinition> configureRssAtomQueries(String username, Long feedId, List<RssAtomUrl> rssAtomFeedUrls) throws DataAccessException, DataUpdateException {
        //
        Map<Long, QueryDefinition> currentQueryDefinitionsById = queryDefinitionDao.findByFeedId(username, feedId, RSS)
                .stream().collect(toMap(QueryDefinition::getId, v -> v));
        //
        List<QueryDefinition> toImport = new ArrayList<>();
        //
        boolean doDelete = false;
        //
        if (isNotEmpty(rssAtomFeedUrls)) {
            List<QueryDefinition> updates = new ArrayList<>();
            List<QueryDefinition> adds = new ArrayList<>();
            for (RssAtomUrl r : rssAtomFeedUrls) {
                String url = r.getFeedUrl();
                if (isBlank(url)) {
                    log.warn("Query is missing a URL, skipping...");
                    continue;
                }
                if (r.getId() != null) {
                    QueryDefinition q = currentQueryDefinitionsById.get(r.getId());
                    if (q != null) {
                        q.setQueryTitle(r.getFeedTitle());
                        q.setQueryImageUrl(r.getFeedImageUrl());
                        q.setQueryText(url);
                        String feedUsername = r.getUsername();
                        String feedPassword = r.getPassword();
                        if (StringUtils.isNotBlank(feedUsername) || StringUtils.isNotBlank(feedPassword)) {
                            q.setQueryConfig(serializeQueryConfig(r));
                        } else {
                            q.setQueryConfig(null);
                        }
                        updates.add(q);
                    } else {
                        String title = r.getFeedTitle();
                        String imageUrl = r.getFeedImageUrl();
                        QueryDefinition newQuery = QueryDefinition.from(feedId, username, title, imageUrl, url, RSS, serializeQueryConfig(r));
                        adds.add(newQuery);
                    }
                } // r.getId() == null case is ignored
            }
            if (isNotEmpty(updates)) {
                // apply updates
                // query_text = ?, query_type = ?, query_config = ?::json where id = ?
                queryDefinitionDao.updateQueries(updates.stream().map(u -> new Object[] {
                        u.getQueryTitle(),
                        u.getQueryImageUrl(),
                        u.getQueryText(),
                        u.getQueryType(),
                        u.getQueryConfig(),
                        u.getId()
                }).collect(toList()));
                // mark for import
                toImport.addAll(updates);
            }
            if (isNotEmpty(adds)) {
                // apply additions
                List<Long> queryIds = queryDefinitionDao.add(adds);
                // re-select
                List<QueryDefinition> newQueries = queryDefinitionDao.findByIds(username, queryIds);
                // mark for import
                toImport.addAll(newQueries);
            }
            // remove already-updated items from currentQueryDefinitionsById
            for (QueryDefinition q : updates) {
                currentQueryDefinitionsById.remove(q.getId());
            }
            currentQueryDefinitionsById.values().removeIf(updates::contains);
            // (the key set now contains Ids of queries that weren't posted back, i.e., deletions)
            doDelete = true;
        } else {
            if (!currentQueryDefinitionsById.isEmpty()) {
                // delete all queries
                doDelete = true;
            }
        }

        if (doDelete && !currentQueryDefinitionsById.keySet().isEmpty()) {
            queryDefinitionDao.deleteQueries(copyOf(currentQueryDefinitionsById.keySet()));
        }

        return toImport;
    }

    private Serializable serializeQueryConfig(RssAtomUrl rssAtomUrl) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", rssAtomUrl.getUsername());
        jsonObject.addProperty("password", rssAtomUrl.getPassword());
        return jsonObject.toString();
    }
}
