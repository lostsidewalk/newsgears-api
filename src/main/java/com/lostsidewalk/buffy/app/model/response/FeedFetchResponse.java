package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.app.model.QueryMetricsWithErrorDetails;
import com.lostsidewalk.buffy.feed.FeedDefinition;
import com.lostsidewalk.buffy.newsapi.NewsApiCategories;
import com.lostsidewalk.buffy.newsapi.NewsApiCountries;
import com.lostsidewalk.buffy.newsapi.NewsApiLanguages;
import com.lostsidewalk.buffy.newsapi.NewsApiSources;
import com.lostsidewalk.buffy.query.QueryDefinition;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FeedFetchResponse {
    List<FeedDefinition> feedDefinitions;
    Map<Long, List<QueryDefinition>> queryDefinitions;
    Map<Long, List<QueryMetricsWithErrorDetails>> queryMetrics;
    Map<NewsApiSources, Map<String, String>> allNewsApiV2Sources; // ea. NewsApiV2 source w/attributes
    List<NewsApiCountries> allNewsApiV2Countries;
    List<NewsApiCategories> allNewsApiV2Categories;
    List<NewsApiLanguages> allNewsApiV2Languages;

    private FeedFetchResponse(List<FeedDefinition> feedDefinitions,
                              Map<Long, List<QueryDefinition>> queryDefinitions, // mapped by feedId
                              Map<Long, List<QueryMetricsWithErrorDetails>> queryMetrics, // mapped by queryId
                              Map<NewsApiSources, Map<String, String>> allNewsApiV2Sources,
                              List<NewsApiCountries> allNewsApiV2Countries,
                              List<NewsApiCategories> allNewsApiV2Categories,
                              List<NewsApiLanguages> allNewsApiV2Languages)
    {
        this.feedDefinitions = feedDefinitions;
        this.queryDefinitions = queryDefinitions;
        this.queryMetrics = queryMetrics;
        this.allNewsApiV2Sources = allNewsApiV2Sources;
        this.allNewsApiV2Countries = allNewsApiV2Countries;
        this.allNewsApiV2Categories = allNewsApiV2Categories;
        this.allNewsApiV2Languages = allNewsApiV2Languages;
    }

    public static FeedFetchResponse from(List<FeedDefinition> feedDefinitions,
                                         Map<Long, List<QueryDefinition>> queryDefinitions, // mapped by feedId
                                         Map<Long, List<QueryMetricsWithErrorDetails>> queryMetrics, // mapped by queryId
                                         Map<NewsApiSources, Map<String, String>> allNewsApiV2Sources,
                                         List<NewsApiCountries> allNewsApiV2Countries,
                                         List<NewsApiCategories> allNewsApiV2Categories,
                                         List<NewsApiLanguages> allNewsApiV2Languages)
    {
        return new FeedFetchResponse(
                feedDefinitions,
                queryDefinitions,
                queryMetrics,
                allNewsApiV2Sources,
                allNewsApiV2Countries,
                allNewsApiV2Categories,
                allNewsApiV2Languages
        );
    }
}
