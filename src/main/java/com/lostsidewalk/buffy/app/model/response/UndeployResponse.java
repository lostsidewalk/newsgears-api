package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

@Data
public class UndeployResponse {
    String feedIdent;

    private UndeployResponse(String feedIdent) {
        this.feedIdent = feedIdent;
    }

    public static UndeployResponse from(String feedIdent) {
        return new UndeployResponse(feedIdent);
    }
}
