package com.lostsidewalk.buffy.app.model.request;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class UpdateSubscriptionRequest {

    @NotNull(message = "{update.subscription.error.status-is-null}")
    SubscriptionStatus subscriptionStatus;
}
