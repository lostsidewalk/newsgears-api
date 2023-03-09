package com.lostsidewalk.buffy.app.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.query.QueryDefinition;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Data
@JsonInclude(NON_EMPTY)
public class ThumbnailedQueryDefinition implements Serializable {

    @Serial
    private static final long serialVersionUID = 98732456987L;

    final QueryDefinition queryDefinition;

    String queryDefinitionImageUrl;

    ThumbnailedQueryDefinition(QueryDefinition queryDefinition) {
        this.queryDefinition = queryDefinition;
    }

    public static ThumbnailedQueryDefinition from(QueryDefinition queryDefinition, String queryDefinitionImageUrl) {
        ThumbnailedQueryDefinition t = new ThumbnailedQueryDefinition(queryDefinition);
        t.queryDefinitionImageUrl = queryDefinitionImageUrl;

        return t;
    }
}
