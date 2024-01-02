package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.queue.QueueDefinition;
import com.lostsidewalk.buffy.rule.RuleSet;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QueueFetchResponse {
    List<QueueDefinition> queueDefinitions;
    Map<Long, List<ThumbnailedSubscriptionDefinition>> subscriptionDefinitions;
    Map<Long, List<SubscriptionMetricsWithErrorDetails>> subscriptionMetrics;
    Map<Long, RuleSet> queueImportRuleSets;
    Map<Long, RuleSet> subscriptionImportRuleSets;

    private QueueFetchResponse(List<QueueDefinition> queueDefinitions,
                               Map<Long, List<ThumbnailedSubscriptionDefinition>> subscriptionDefinitions,
                               Map<Long, List<SubscriptionMetricsWithErrorDetails>> subscriptionMetrics,
                               Map<Long, RuleSet> queueImportRuleSets,
                               Map<Long, RuleSet> subscriptionImportRuleSets)
    {
        this.queueDefinitions = queueDefinitions;
        this.subscriptionDefinitions = subscriptionDefinitions;
        this.subscriptionMetrics = subscriptionMetrics;
        this.queueImportRuleSets = queueImportRuleSets;
        this.subscriptionImportRuleSets = subscriptionImportRuleSets;
    }

    public static QueueFetchResponse from(List<QueueDefinition> queueDefinitions,
                                          Map<Long, List<ThumbnailedSubscriptionDefinition>> subscriptionDefinitions, // mapped by queueId
                                          Map<Long, List<SubscriptionMetricsWithErrorDetails>> subscriptionMetrics, // mapped by subscriptionId
                                          Map<Long, RuleSet> queueImportRuleSets, // mapped by queueId
                                          Map<Long, RuleSet> subscriptionImportRuleSets // mapped by subscriptionId
                                    )
    {
        return new QueueFetchResponse(
                queueDefinitions,
                subscriptionDefinitions,
                subscriptionMetrics,
                queueImportRuleSets,
                subscriptionImportRuleSets
        );
    }
}
