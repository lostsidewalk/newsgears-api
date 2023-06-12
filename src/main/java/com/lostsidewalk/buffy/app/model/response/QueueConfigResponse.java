package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.queue.QueueDefinition;
import lombok.Data;

import java.util.List;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;

@Data
public class QueueConfigResponse {

    QueueDefinition queueDefinition;
    List<ThumbnailedSubscriptionDefinition> subscriptionDefinitions;
    String queueImgSrc;

    private QueueConfigResponse(QueueDefinition queueDefinition, List<ThumbnailedSubscriptionDefinition> subscriptionDefinitions) {
        this.queueDefinition = queueDefinition;
        this.subscriptionDefinitions = subscriptionDefinitions;
    }

    public static QueueConfigResponse from(QueueDefinition queueDefinition, List<ThumbnailedSubscriptionDefinition> subscriptionDefinitions, byte[] feedImg) {
        QueueConfigResponse f = new QueueConfigResponse(queueDefinition, subscriptionDefinitions);
        f.queueImgSrc = encodeBase64String(feedImg);

        return f;
    }
}
