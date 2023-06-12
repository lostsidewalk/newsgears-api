package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

import java.util.List;

@Data
public class SubscriptionConfigResponse {

    List<ThumbnailedSubscriptionDefinition> subscriptionDefinitions;

    private SubscriptionConfigResponse(List<ThumbnailedSubscriptionDefinition> subscriptionDefinitions) {
        this.subscriptionDefinitions = subscriptionDefinitions;
    }

    public static SubscriptionConfigResponse from(List<ThumbnailedSubscriptionDefinition> subscriptionDefinitions) {
        return new SubscriptionConfigResponse(subscriptionDefinitions);
    }
}
