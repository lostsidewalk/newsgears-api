package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.app.model.QueryMetricsWithErrorDetails;
import com.lostsidewalk.buffy.feed.FeedDefinition;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FeedFetchResponse {
    List<FeedDefinition> feedDefinitions;
    Map<Long, List<ThumbnailedQueryDefinition>> queryDefinitions;
    Map<Long, List<QueryMetricsWithErrorDetails>> queryMetrics;

    private FeedFetchResponse(List<FeedDefinition> feedDefinitions,
                              Map<Long, List<ThumbnailedQueryDefinition>> queryDefinitions,
                              Map<Long, List<QueryMetricsWithErrorDetails>> queryMetrics)
    {
        this.feedDefinitions = feedDefinitions;
        this.queryDefinitions = queryDefinitions;
        this.queryMetrics = queryMetrics;
    }

    public static FeedFetchResponse from(List<FeedDefinition> feedDefinitions,
                                         Map<Long, List<ThumbnailedQueryDefinition>> queryDefinitions, // mapped by feedId
                                         Map<Long, List<QueryMetricsWithErrorDetails>> queryMetrics // mapped by queryId
                                    )
    {
        return new FeedFetchResponse(
                feedDefinitions,
                queryDefinitions,
                queryMetrics
        );
    }
}
