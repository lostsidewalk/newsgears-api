package com.lostsidewalk.buffy.app.query;

import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
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
import java.util.Objects;

import static com.lostsidewalk.buffy.rss.RssImporter.RSS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
public class QueryDefinitionService {

    @Autowired
    QueryDefinitionDao queryDefinitionDao;

    public QueryDefinition findById(String username, Long id) throws DataAccessException {
        return queryDefinitionDao.findById(username, id);
    }

    public List<QueryDefinition> findByUsername(String username) throws DataAccessException {
        return queryDefinitionDao.findByUsername(username);
    }

    public List<QueryDefinition> findByFeedId(String username, Long id) throws DataAccessException {
        return queryDefinitionDao.findByFeedId(username, id);
    }
    //
    // called by feed definition controller
    //
    public List<QueryDefinition> updateQueries(String username, Long feedId, List<RssAtomUrl> rssAtomFeedUrls) throws DataAccessException {
        //
        Map<Long, QueryDefinition> currentQueryDefinitionsById = queryDefinitionDao.findByFeedId(username, feedId, RSS)
                .stream().collect(toMap(QueryDefinition::getId, v -> v));
        //
        List<QueryDefinition> updatedQueries = new ArrayList<>();
        //
        if (isNotEmpty(rssAtomFeedUrls)) {
            for (RssAtomUrl r : rssAtomFeedUrls) {
                if (r.getId() != null) {
                    QueryDefinition q = currentQueryDefinitionsById.get(r.getId());
                    if (q != null) {
                        if (needsUpdate(r, q)) {
                            q.setQueryTitle(r.getFeedTitle());
                            q.setQueryImageUrl(r.getFeedImageUrl());
                            q.setQueryText(r.getFeedUrl());
                            String feedUsername = r.getUsername();
                            String feedPassword = r.getPassword();
                            if (StringUtils.isNotBlank(feedUsername) || StringUtils.isNotBlank(feedPassword)) {
                                q.setQueryConfig(serializeQueryConfig(r));
                            } else {
                                q.setQueryConfig(null);
                            }
                            updatedQueries.add(q);
                        }
                    }
                } // r.getId() == null case is ignored
            }
            if (isNotEmpty(updatedQueries)) {
                // apply updates
                // query_text = ?, query_type = ?, query_config = ?::json where id = ?
                queryDefinitionDao.updateQueries(updatedQueries.stream().map(u -> new Object[] {
                        u.getQueryTitle(),
                        u.getQueryImageUrl(),
                        u.getQueryText(),
                        u.getQueryType(),
                        u.getQueryConfig(),
                        u.getId()
                }).collect(toList()));
            }
        }

        return updatedQueries;
    }
    //
    // called by query creation task processor
    //
    List<QueryDefinition> addQueries(String username, Long feedId, List<RssAtomUrl> rssAtomUrls) throws DataAccessException, DataUpdateException {
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
    //
    // called by feed definition controller
    //
    public List<QueryDefinition> createQueries(String username, Long feedId, List<RssAtomUrl> rssAtomFeedUrls) throws DataAccessException, DataUpdateException {
        List<QueryDefinition> createdQueries = new ArrayList<>();
        if (isNotEmpty(rssAtomFeedUrls)) {
            List<QueryDefinition> toImport = new ArrayList<>();
            if (isNotEmpty(rssAtomFeedUrls)) {
                List<QueryDefinition> adds = new ArrayList<>();
                for (RssAtomUrl r : rssAtomFeedUrls) {
                    QueryDefinition newQuery = QueryDefinition.from(feedId, username, r.getFeedTitle(), r.getFeedImageUrl(), r.getFeedUrl(), RSS, serializeQueryConfig(r));
                    adds.add(newQuery);
                }
                if (isNotEmpty(adds)) {
                    // apply additions
                    List<Long> queryIds = queryDefinitionDao.add(adds);
                    // re-select
                    List<QueryDefinition> newQueries = queryDefinitionDao.findByIds(username, queryIds);
                    // mark for import
                    toImport.addAll(newQueries);
                }
            }

            createdQueries.addAll(toImport);
        }

        return createdQueries;
    }
    //
    // called by feed definition controller
    //
    public void deleteQueryById(String username, Long feedId, Long queryId) throws DataAccessException, DataUpdateException {
        // delete this query
        queryDefinitionDao.deleteById(username, feedId, queryId);
    }

    private boolean needsUpdate(RssAtomUrl rssAtomUrl, QueryDefinition q) {
        if (!StringUtils.equals(q.getQueryTitle(), rssAtomUrl.getFeedTitle())) {
            return true;
        }
        if (!StringUtils.equals(q.getQueryImageUrl(), rssAtomUrl.getFeedImageUrl())) {
            return true;
        }
        if (!StringUtils.equals(q.getQueryText(), rssAtomUrl.getFeedUrl())) {
            return true;
        }
        if (!StringUtils.equals(q.getQueryTitle(), rssAtomUrl.getFeedTitle())) {
            return true;
        }
        if (!StringUtils.equals(q.getQueryTitle(), rssAtomUrl.getFeedTitle())) {
            return true;
        }
        return !Objects.equals(serializeQueryConfig(rssAtomUrl), q.getQueryConfig());
    }

    private Serializable serializeQueryConfig(RssAtomUrl rssAtomUrl) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", rssAtomUrl.getUsername());
        jsonObject.addProperty("password", rssAtomUrl.getPassword());
        return jsonObject.toString();
    }
}
