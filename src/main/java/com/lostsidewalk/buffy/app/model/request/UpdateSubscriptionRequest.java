package com.lostsidewalk.buffy.app.model.request;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class UpdateSubscriptionRequest {
    @NotNull
    SubscriptionStatus subscriptionStatus;
}
