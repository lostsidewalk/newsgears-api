package com.lostsidewalk.buffy.app.query;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.request.FeedConfigRequest;
import com.lostsidewalk.buffy.app.model.request.RssAtomUrl;
import com.lostsidewalk.buffy.newsapi.NewsApiCategories;
import com.lostsidewalk.buffy.newsapi.NewsApiCountries;
import com.lostsidewalk.buffy.newsapi.NewsApiLanguages;
import com.lostsidewalk.buffy.newsapi.NewsApiSources;
import com.lostsidewalk.buffy.query.QueryDefinition;
import com.lostsidewalk.buffy.query.QueryDefinitionDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lostsidewalk.buffy.newsapi.NewsApiImporter.NEWSAPIV2_EVERYTHING;
import static com.lostsidewalk.buffy.newsapi.NewsApiImporter.NEWSAPIV2_HEADLINES;
import static com.lostsidewalk.buffy.rss.RssImporter.RSS;
import static java.util.Collections.singletonList;
import static java.util.List.copyOf;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
public class QueryDefinitionService {

    @Autowired
    QueryDefinitionDao queryDefinitionDao;

    public List<NewsApiSources> getAllNewsApiV2Sources() {
        return List.of(NewsApiSources.values());
    }

    public QueryDefinition configureNewsApiV2Query(String username,
                                                   String feedIdent,
                                                   String newsApiV2QueryText,
                                                   List<NewsApiSources> newsApiV2Sources,
                                                   NewsApiLanguages newsApiV2Language,
                                                   NewsApiCountries newsApiV2Country,
                                                   NewsApiCategories newsApiV2Category)
            throws DataAccessException, DataUpdateException {
        if (isNotBlank(newsApiV2QueryText) || isNotEmpty(newsApiV2Sources) || newsApiV2Language != null || newsApiV2Country != null || newsApiV2Category != null) {
            boolean doImport;
            List<QueryDefinition> currentQueries = queryDefinitionDao.findByFeedIdent(username, feedIdent, NEWSAPIV2_HEADLINES);
            if (isNotEmpty(currentQueries)) {
                // update the existing query
                QueryDefinition currentQuery = currentQueries.get(0);
                queryDefinitionDao.updateQueries(singletonList(new Object[] {
                        newsApiV2QueryText,
                        NEWSAPIV2_HEADLINES,
                        serializeQueryConfig(newsApiV2Sources, newsApiV2Language, newsApiV2Country, newsApiV2Category),
                        currentQuery.getId()}));
                doImport = isNotBlank(newsApiV2QueryText) && !newsApiV2QueryText.equals(currentQuery.getQueryText());
            } else {
                // add a new query
                QueryDefinition newQuery = QueryDefinition.from(feedIdent, username, newsApiV2QueryText, NEWSAPIV2_HEADLINES,
                        serializeQueryConfig(newsApiV2Sources, newsApiV2Language, newsApiV2Country, newsApiV2Category));
                queryDefinitionDao.add(newQuery);
                doImport = true;
            }
            // kick off the initial (or newly updated) import
            if (doImport) {
                return QueryDefinition.from(feedIdent, username, newsApiV2QueryText, NEWSAPIV2_EVERYTHING,
                        serializeQueryConfig(newsApiV2Sources, newsApiV2Language, newsApiV2Country, newsApiV2Category));
            }
        }

        return null;
    }

    private static final Gson GSON = new Gson();

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

    public List<QueryDefinition> configureRssAtomQueries(String username, String feedIdent, List<RssAtomUrl> rssAtomFeedUrls) throws DataAccessException, DataUpdateException {

        Map<Long, QueryDefinition> currentQueryDefinitionsById = queryDefinitionDao.findByFeedIdent(username, feedIdent, RSS)
                .stream().collect(Collectors.toMap(QueryDefinition::getId, v -> v));

        List<QueryDefinition> toImport = new ArrayList<>();
        boolean doDelete = false;
        if (isNotEmpty(rssAtomFeedUrls)) {
            List<QueryDefinition> updates = new ArrayList<>();
            List<QueryDefinition> adds = new ArrayList<>();
            for (RssAtomUrl r : rssAtomFeedUrls) {
                if (r.getId() != null) {
                    QueryDefinition q = currentQueryDefinitionsById.get(r.getId());
                    if (q != null) {
                        if (r.getUrl().equals(q.getQueryText())) {
                            currentQueryDefinitionsById.remove(r.getId());
                        } else {
                            q.setQueryText(r.getUrl());
                            updates.add(q);
                        }
                    } else {
                        adds.add(QueryDefinition.from(feedIdent, username, r.getUrl(), RSS, null));
                    }
                } // r.getId() == null case is ignored
            }
            if (isNotEmpty(updates)) {
                // apply updates
                // query_text = ?, query_type = ?, query_config = ?::json where id = ?
                queryDefinitionDao.updateQueries(updates.stream().map(u -> new Object[] {
                    u.getQueryText(), u.getQueryType(), u.getQueryConfig(), u.getId()
                }).collect(toList()));
                // mark for import
                toImport.addAll(updates);
            }
            if (isNotEmpty(adds)) {
                // apply additions
                queryDefinitionDao.add(adds);
                // mark for import
                toImport.addAll(adds);
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

    public List<QueryDefinition> findByFeedIdent(String username, String ident) throws DataAccessException {
        return queryDefinitionDao.findByFeedIdent(username, ident);
    }

    public List<QueryDefinition> updateQueries(String username, FeedConfigRequest feedConfigRequest) throws DataAccessException, DataUpdateException {
        List<QueryDefinition> updatedQueries = new ArrayList<>();
        // add/update the NEWSAPIV2 query
        QueryDefinition everythingQuery = configureNewsApiV2Query(username,
                feedConfigRequest.getIdent(),
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
        List<QueryDefinition> rssAtomQueries = configureRssAtomQueries(username, feedConfigRequest.getIdent(), feedConfigRequest.getRssAtomFeedUrls());
        if (isNotEmpty(rssAtomQueries)) {
            updatedQueries.addAll(rssAtomQueries);
        }

        return updatedQueries;
    }

    public List<QueryDefinition> createQueries(String username, FeedConfigRequest feedConfigRequest) throws DataAccessException, DataUpdateException {
        List<QueryDefinition> createdQueries = new ArrayList<>();
        String newsApiV2QueryText = feedConfigRequest.getNewsApiV2QueryText();
        if (isNotBlank(newsApiV2QueryText)) {
            QueryDefinition q = configureNewsApiV2Query(username,
                    feedConfigRequest.getIdent(),
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
            createdQueries.addAll(configureRssAtomQueries(username, feedConfigRequest.getIdent(), rssAtomUrls));
        }

        return createdQueries;
    }
}
