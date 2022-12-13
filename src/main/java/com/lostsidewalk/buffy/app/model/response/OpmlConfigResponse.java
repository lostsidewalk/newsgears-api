package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.app.model.request.FeedConfigRequest;
import lombok.Data;

import java.util.List;

@Data
public class OpmlConfigResponse {
    List<FeedConfigRequest> feedConfigRequests;
    List<String> errors;

    OpmlConfigResponse(List<FeedConfigRequest> feedConfigRequests, List<String> errors) {
        this.feedConfigRequests = feedConfigRequests;
        this.errors = errors;
    }

    public static OpmlConfigResponse from(List<FeedConfigRequest> feedConfigRequests, List<String> errors) {
        return new OpmlConfigResponse(feedConfigRequests, errors);
    }
}
