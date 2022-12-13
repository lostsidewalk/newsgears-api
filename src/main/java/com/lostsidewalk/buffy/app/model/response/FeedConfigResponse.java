package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.feed.FeedDefinition;
import com.lostsidewalk.buffy.query.QueryDefinition;
import lombok.Data;

import java.util.List;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;

@Data
public class FeedConfigResponse {
    String feedImageSrc;
    FeedDefinition feedDefinition;
    List<QueryDefinition> queryDefinitions;
    String feedImgSrc;

    private FeedConfigResponse(FeedDefinition feedDefinition, List<QueryDefinition> queryDefinitions) {
        this.feedDefinition = feedDefinition;
        this.queryDefinitions = queryDefinitions;
    }

    public static FeedConfigResponse from(FeedDefinition feedDefinition, List<QueryDefinition> queryDefinitions, byte[] feedImg) {
        FeedConfigResponse f = new FeedConfigResponse(feedDefinition, queryDefinitions);
        f.feedImgSrc = encodeBase64String(feedImg);

        return f;
    }
}
