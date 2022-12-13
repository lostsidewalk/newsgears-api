package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.Publisher;
import lombok.Data;

import java.util.Date;

@Data
public class DeployResponse {
    String publisherIdent;
    String feedIdent;
    Date timestamp;

    private DeployResponse(String publisherIdent, String feedIdent, Date timestamp) {
        this.publisherIdent = publisherIdent;
        this.feedIdent = feedIdent;
        this.timestamp = timestamp;
    }

    public static DeployResponse from(Publisher.PubResult pubResult) {
        String publisherIdent = pubResult.getPublisherIdent();
        String feedIdent = pubResult.getFeedIdent();
        return new DeployResponse(publisherIdent, feedIdent, pubResult.getPubDate());
    }
}
