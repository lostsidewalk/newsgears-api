package com.lostsidewalk.buffy.app.broker;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.feed.QueueDefinitionService;
import com.lostsidewalk.buffy.app.model.request.QueueConfigRequest;
import com.lostsidewalk.buffy.app.model.request.SubscriptionConfigRequest;
import com.lostsidewalk.buffy.app.model.response.QueueConfigResponse;
import com.lostsidewalk.buffy.app.model.response.ThumbnailedSubscriptionDefinition;
import com.lostsidewalk.buffy.app.proxy.ProxyService;
import com.lostsidewalk.buffy.app.query.SubscriptionDefinitionService;
import com.lostsidewalk.buffy.app.resolution.FeedResolutionService;
import com.lostsidewalk.buffy.app.thumbnail.ThumbnailService;
import com.lostsidewalk.buffy.model.RenderedThumbnail;
import com.lostsidewalk.buffy.post.PostImporter;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import com.lostsidewalk.buffy.subscription.SubscriptionDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Component
public class OpmlUploadHandler implements MessageHandler<List<QueueConfigResponse>> {

    private static final Gson GSON = new Gson();

    @SuppressWarnings("EmptyClass")
    private final Type LIST_OF_QUEUE_CONFIG_REQUESTS = new TypeToken<ArrayList<QueueConfigRequest>>() {}.getType();

    @Autowired
    QueueDefinitionService queueDefinitionService;

    @Autowired
    SubscriptionDefinitionService subscriptionDefinitionService;

    @Autowired
    ThumbnailService thumbnailService;

    @Autowired
    ProxyService proxyService;

    @Autowired
    FeedResolutionService feedResolutionService;

    @Autowired
    BlockingQueue<SubscriptionCreationTask> creationTaskQueue;

    @Autowired
    PostImporter postImporter;

    @Override
    public String getResponseType() {
        return "CREATED_QUEUE_DEFINITIONS";
    }

    @Override
    public final List<QueueConfigResponse> handleMessage(JsonElement payload, String username, String destination)
            throws DataConflictException, DataAccessException, DataUpdateException
    {
        if (!payload.isJsonArray()) {
            log.error("Invalid payload for OPML upload message, payload={}", payload);
            return null;
        }
        // deserialize payload into a List<QueueConfigRequest>
        List<QueueConfigRequest> queueConfigRequests = GSON.fromJson(payload, LIST_OF_QUEUE_CONFIG_REQUESTS);
        //
        List<QueueConfigResponse> queueConfigResponses = newArrayList();
        // for ea. feed config request
        for (QueueConfigRequest queueConfigRequest : queueConfigRequests) {
            // create the feed
            Long queueId = queueDefinitionService.createFeed(username, queueConfigRequest);
            List<SubscriptionConfigRequest> subscriptions = queueConfigRequest.getSubscriptions();
            if (isNotEmpty(subscriptions)) {
                for (SubscriptionConfigRequest subscription : subscriptions) {
                    try {
                        addSubscriptionsToQueue(singletonList(subscription), username, destination, queueId);
                    } catch (DataConflictException | DataUpdateException | DataAccessException e) {
                        log.warn("Unable to add subscription due to: {}", e.getMessage()); // TODO: fix this
                    }
                }
            }
            // re-fetch this feed definition
            QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
            // re-fetch query definitions for this feed
            List<ThumbnailedSubscriptionDefinition> subscriptionDefinitions = addThumbnails(subscriptionDefinitionService.findByQueueId(username, queueId));
            // build feed config responses to return the front-end
            queueConfigResponses.add(QueueConfigResponse.from(
                    queueDefinition,
                    subscriptionDefinitions,
                    buildThumbnail(queueDefinition))
            );
        }
        return queueConfigResponses;
    }

    private void addSubscriptionsToQueue(List<SubscriptionConfigRequest> subscriptions, String username, String destination, Long queueId)
            throws DataConflictException, DataAccessException, DataUpdateException
    {
        creationTaskQueue.add(new SubscriptionCreationTask(subscriptions, username, queueId, destination));
    }

    private List<ThumbnailedSubscriptionDefinition> addThumbnails(Iterable<? extends SubscriptionDefinition> subscriptionDefinitions) {
        List<ThumbnailedSubscriptionDefinition> responses = newArrayListWithCapacity(size(subscriptionDefinitions));
        for (SubscriptionDefinition subscriptionDefinition : subscriptionDefinitions) {
            responses.add(addThumbnail(subscriptionDefinition));
        }
        return responses;
    }

    private ThumbnailedSubscriptionDefinition addThumbnail(SubscriptionDefinition q) {
        String imageProxyUrl = buildThumbnailProxyUrl(q);
        return ThumbnailedSubscriptionDefinition.from(q, imageProxyUrl);
    }

    private String buildThumbnailProxyUrl(SubscriptionDefinition q) {
        if (isNotBlank(q.getImgUrl())) {
            return proxyService.rewriteImageUrl(q.getImgUrl(), EMPTY);
        }

        return null;
    }

    private byte[] buildThumbnail(QueueDefinition queueDefinition) throws DataAccessException {
        if (isNotBlank(queueDefinition.getQueueImgSrc())) {
            String transportIdent = queueDefinition.getQueueImgTransportIdent();
            byte[] image = ofNullable(thumbnailService.getThumbnail(transportIdent)).map(RenderedThumbnail::getImage).orElse(null);
            if (image == null) {
                image = ofNullable(thumbnailService.refreshThumbnailFromSrc(transportIdent, queueDefinition.getQueueImgSrc()))
                        .map(RenderedThumbnail::getImage)
                        .orElse(null);
            }

            return image;
        }

        return null;
    }

    @Override
    public String toString() {
        return "OpmlUploadHandler{" +
                "LIST_OF_QUEUE_CONFIG_REQUESTS=" + LIST_OF_QUEUE_CONFIG_REQUESTS +
                ", queueDefinitionService=" + queueDefinitionService +
                ", subscriptionDefinitionService=" + subscriptionDefinitionService +
                ", thumbnailService=" + thumbnailService +
                ", proxyService=" + proxyService +
                ", feedResolutionService=" + feedResolutionService +
                ", creationTaskQueue=" + creationTaskQueue +
                ", postImporter=" + postImporter +
                '}';
    }
}
