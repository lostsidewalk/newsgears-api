package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.queue.QueueDefinition;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QueueFetchResponse {
    List<QueueDefinition> queueDefinitions;
    Map<Long, List<ThumbnailedSubscriptionDefinition>> subscriptionDefinitions;
    Map<Long, List<SubscriptionMetricsWithErrorDetails>> subscriptionMetrics;

    private QueueFetchResponse(List<QueueDefinition> queueDefinitions,
                               Map<Long, List<ThumbnailedSubscriptionDefinition>> subscriptionDefinitions,
                               Map<Long, List<SubscriptionMetricsWithErrorDetails>> subscriptionMetrics)
    {
        this.queueDefinitions = queueDefinitions;
        this.subscriptionDefinitions = subscriptionDefinitions;
        this.subscriptionMetrics = subscriptionMetrics;
    }

    public static QueueFetchResponse from(List<QueueDefinition> queueDefinitions,
                                          Map<Long, List<ThumbnailedSubscriptionDefinition>> subscriptionDefinitions, // mapped by queueId
                                          Map<Long, List<SubscriptionMetricsWithErrorDetails>> subscriptionMetrics // mapped by subscriptionId
                                    )
    {
        return new QueueFetchResponse(
                queueDefinitions,
                subscriptionDefinitions,
                subscriptionMetrics
        );
    }
}
