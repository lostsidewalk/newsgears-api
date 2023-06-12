package com.lostsidewalk.buffy.app.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.subscription.SubscriptionDefinition;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Data
@JsonInclude(NON_EMPTY)
public class ThumbnailedSubscriptionDefinition implements Serializable {

    @Serial
    private static final long serialVersionUID = 98732456987L;

    final SubscriptionDefinition subscriptionDefinition;

    String subscriptionDefinitionImageUrl;

    ThumbnailedSubscriptionDefinition(SubscriptionDefinition subscriptionDefinition) {
        this.subscriptionDefinition = subscriptionDefinition;
    }

    public static ThumbnailedSubscriptionDefinition from(SubscriptionDefinition subscriptionDefinition, String subscriptionDefinitionImageUrl) {
        ThumbnailedSubscriptionDefinition t = new ThumbnailedSubscriptionDefinition(subscriptionDefinition);
        t.subscriptionDefinitionImageUrl = subscriptionDefinitionImageUrl;

        return t;
    }
}
