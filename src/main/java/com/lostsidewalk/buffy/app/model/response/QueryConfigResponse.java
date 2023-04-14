package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

import java.util.List;

@Data
public class QueryConfigResponse {

    List<ThumbnailedQueryDefinition> queryDefinitions;

    private QueryConfigResponse(List<ThumbnailedQueryDefinition> queryDefinitions) {
        this.queryDefinitions = queryDefinitions;
    }

    public static QueryConfigResponse from(List<ThumbnailedQueryDefinition> queryDefinitions) {
        return new QueryConfigResponse(queryDefinitions);
    }
}
