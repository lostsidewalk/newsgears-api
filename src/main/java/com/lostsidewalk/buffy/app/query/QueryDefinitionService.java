package com.lostsidewalk.buffy.app.query;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.request.FeedConfigRequest;
import com.lostsidewalk.buffy.app.model.request.RssAtomUrl;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.newsapi.NewsApiCategories;
import com.lostsidewalk.buffy.newsapi.NewsApiCountries;
import com.lostsidewalk.buffy.newsapi.NewsApiLanguages;
import com.lostsidewalk.buffy.newsapi.NewsApiSources;
import com.lostsidewalk.buffy.post.ContentObject;
import com.lostsidewalk.buffy.query.QueryDefinition;
import com.lostsidewalk.buffy.query.QueryDefinitionDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.discoverUrl;
import static com.lostsidewalk.buffy.newsapi.NewsApiImporter.NEWSAPIV2_EVERYTHING;
import static com.lostsidewalk.buffy.newsapi.NewsApiImporter.NEWSAPIV2_HEADLINES;
import static com.lostsidewalk.buffy.rss.RssImporter.RSS;
import static java.util.Collections.singletonList;
import static java.util.List.copyOf;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
public class QueryDefinitionService {

    @Autowired
    QueryDefinitionDao queryDefinitionDao;

    private static final Gson GSON = new Gson();

    public List<QueryDefinition> findByUsername(String username) throws DataAccessException {
        return queryDefinitionDao.findByUsername(username);
    }

    public List<QueryDefinition> findByFeedId(String username, Long id) throws DataAccessException {
        return queryDefinitionDao.findByFeedId(username, id);
    }

    public List<QueryDefinition> updateQueries(String username, Long feedId, FeedConfigRequest feedConfigRequest) throws DataAccessException, DataUpdateException {
        List<QueryDefinition> updatedQueries = new ArrayList<>();
        // add/update the NEWSAPIV2 query
        QueryDefinition everythingQuery = configureNewsApiV2Query(username,
                feedId,
                feedConfigRequest.getNewsApiV2QueryText(),
                feedConfigRequest.getNewsApiV2Sources(),
                feedConfigRequest.getNewsApiV2Language(),
                feedConfigRequest.getNewsApiV2Country(),
                feedConfigRequest.getNewsApiV2Category()
        );
        if (everythingQuery != null) {
            updatedQueries.add(everythingQuery);
        }
        // add/update the RSS/ATOM query URLs
        List<QueryDefinition> rssAtomQueries = configureRssAtomQueries(username, feedId, feedConfigRequest.getRssAtomFeedUrls());
        if (isNotEmpty(rssAtomQueries)) {
            updatedQueries.addAll(rssAtomQueries);
        }

        return updatedQueries;
    }

    public List<QueryDefinition> createQueries(String username, Long feedId, FeedConfigRequest feedConfigRequest) throws DataAccessException, DataUpdateException {
        List<QueryDefinition> createdQueries = new ArrayList<>();
        String newsApiV2QueryText = feedConfigRequest.getNewsApiV2QueryText();
        if (isNotBlank(newsApiV2QueryText)) {
            QueryDefinition q = configureNewsApiV2Query(username,
                    feedId,
                    feedConfigRequest.getNewsApiV2QueryText(),
                    feedConfigRequest.getNewsApiV2Sources(),
                    feedConfigRequest.getNewsApiV2Language(),
                    feedConfigRequest.getNewsApiV2Country(),
                    feedConfigRequest.getNewsApiV2Category());
            if (q != null) {
                createdQueries.add(q);
            }
        }
        List<RssAtomUrl> rssAtomUrls = feedConfigRequest.getRssAtomFeedUrls();
        if (isNotEmpty(rssAtomUrls)) {
            createdQueries.addAll(configureRssAtomQueries(username, feedId, rssAtomUrls));
        }

        return createdQueries;
    }

    private QueryDefinition configureNewsApiV2Query(String username,
                                                    Long feedId,
                                                    String newsApiV2QueryText,
                                                    List<NewsApiSources> newsApiV2Sources,
                                                    NewsApiLanguages newsApiV2Language,
                                                    NewsApiCountries newsApiV2Country,
                                                    NewsApiCategories newsApiV2Category)
            throws DataAccessException, DataUpdateException {
        QueryDefinition toImport = null;
        if (isNotBlank(newsApiV2QueryText) || isNotEmpty(newsApiV2Sources) || newsApiV2Language != null || newsApiV2Country != null || newsApiV2Category != null) {
            List<QueryDefinition> currentQueries = queryDefinitionDao.findByFeedId(username, feedId, NEWSAPIV2_HEADLINES);
            if (isNotEmpty(currentQueries)) {
                // update the existing query
                QueryDefinition currentQuery = currentQueries.get(0);
                queryDefinitionDao.updateQueries(singletonList(new Object[] {
                        newsApiV2QueryText,
                        newsApiV2QueryText,
                        NEWSAPIV2_HEADLINES,
                        serializeQueryConfig(newsApiV2Sources, newsApiV2Language, newsApiV2Country, newsApiV2Category),
                        currentQuery.getId()}));
                if (isNotBlank(newsApiV2QueryText) && !newsApiV2QueryText.equals(currentQuery.getQueryText())) {
                    toImport = currentQuery;
                }
            } else {
                // add a new query
                Long queryId = queryDefinitionDao.add(QueryDefinition.from(
                        feedId,
                        username,
                        newsApiV2QueryText,
                        newsApiV2QueryText,
                        NEWSAPIV2_HEADLINES,
                        serializeQueryConfig(newsApiV2Sources, newsApiV2Language, newsApiV2Country, newsApiV2Category)
                ));
                toImport = queryDefinitionDao.findById(username, queryId);
            }
        }

        if (toImport != null) {
            toImport.setQueryType(NEWSAPIV2_EVERYTHING);
        }

        return toImport;
    }

    private Serializable serializeQueryConfig(List<NewsApiSources> newsApiV2Sources,
                                              NewsApiLanguages newsApiLanguage, NewsApiCountries newsApiCountry, NewsApiCategories newsApiCategory)
    {
        JsonObject queryConfig = new JsonObject();
        if (isNotEmpty(newsApiV2Sources)) {
            queryConfig.add("sources", GSON.toJsonTree(newsApiV2Sources));
        }
        if (newsApiLanguage != null) {
            queryConfig.addProperty("language", newsApiLanguage.name());
        }
        if (newsApiCountry != null) {
            queryConfig.addProperty("country", newsApiCountry.name());
        }
        if (newsApiCategory != null) {
            queryConfig.addProperty("category", newsApiCategory.name());
        }

        return queryConfig.toString();
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
                if (r.getId() != null) {
                    QueryDefinition q = currentQueryDefinitionsById.get(r.getId());
                    if (q != null) {
                        if (r.getFeedUrl().equals(q.getQueryText())) {
                            currentQueryDefinitionsById.remove(r.getId());
                        } else {
                            String url = r.getFeedUrl();
                            q.setQueryText(url);
                            q.setQueryTitle(discoverFeedTitle(url));
                            updates.add(q);
                        }
                    } else {
                        String url = r.getFeedUrl();
                        adds.add(QueryDefinition.from(feedId, username, discoverFeedTitle(url), url, RSS, null));
                    }
                } // r.getId() == null case is ignored
            }
            if (isNotEmpty(updates)) {
                // apply updates
                // query_text = ?, query_type = ?, query_config = ?::json where id = ?
                queryDefinitionDao.updateQueries(updates.stream().map(u -> new Object[] {
                        u.getQueryTitle(), u.getQueryText(), u.getQueryType(), u.getQueryConfig(), u.getId()
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
            // (the keyset now contains Ids of queries that weren't posted back, i.e., deletions)
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

    private String discoverFeedTitle(String url) {
        String title;
        try {
            FeedDiscoveryInfo feedDiscoveryInfo = discoverUrl(url);
            ContentObject titleObj = feedDiscoveryInfo.getTitle();
            title = titleObj.getValue(); // TODO: might be worth paying attention to 'type', and constructing the title accordingly
        } catch (Exception e) {
            log.debug("Unable to perform feed title discovery due to: {}", e.getMessage());
            title = url;
        }

        return title;
    }
}
