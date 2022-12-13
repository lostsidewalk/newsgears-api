package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

@Data
public class StripeResponse {
    private String sessionId;

    private String sessionUrl;

    StripeResponse(String sessionId, String sessionUrl) {
        this.sessionId = sessionId;
        this.sessionUrl = sessionUrl;
    }

    public static StripeResponse from(String sessionId, String sessionUrl) {
        return new StripeResponse(sessionId, sessionUrl);
    }
}
