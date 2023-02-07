package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.app.model.QueryMetricsWithErrorDetails;
import com.lostsidewalk.buffy.feed.FeedDefinition;
import com.lostsidewalk.buffy.query.QueryDefinition;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;

@Data
public class FeedConfigResponse {

    FeedDefinition feedDefinition;
    List<QueryDefinition> queryDefinitions;
    Map<Long, List<QueryMetricsWithErrorDetails>> queryMetrics;
    String feedImgSrc;

    private FeedConfigResponse(FeedDefinition feedDefinition, List<QueryDefinition> queryDefinitions, Map<Long, List<QueryMetricsWithErrorDetails>> queryMetrics) {
        this.feedDefinition = feedDefinition;
        this.queryDefinitions = queryDefinitions;
        this.queryMetrics = queryMetrics;
    }

    public static FeedConfigResponse from(FeedDefinition feedDefinition, List<QueryDefinition> queryDefinitions, Map<Long, List<QueryMetricsWithErrorDetails>> queryMetrics, byte[] feedImg) {
        FeedConfigResponse f = new FeedConfigResponse(feedDefinition, queryDefinitions, queryMetrics);
        f.feedImgSrc = encodeBase64String(feedImg);

        return f;
    }
}
