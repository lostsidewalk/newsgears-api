package com.lostsidewalk.buffy.app.broker;

import com.lostsidewalk.buffy.app.model.request.SubscriptionConfigRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SubscriptionCreationTask {

    List<SubscriptionConfigRequest> subscriptions;

    String username;

    Long queueId;

    String destination;
}
