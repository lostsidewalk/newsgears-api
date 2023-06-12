package com.lostsidewalk.buffy.app.query;

import com.lostsidewalk.buffy.app.model.request.Subscription;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SubscriptionCreationTask {

    List<Subscription> subscriptions;

    String username;

    Long queueId;
}
