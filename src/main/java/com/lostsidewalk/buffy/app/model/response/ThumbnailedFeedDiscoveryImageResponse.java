package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;

@Data
public class ThumbnailedFeedDiscoveryImageResponse {

    String title;
    String description;
    Integer height;
    Integer width;
    String link;
    String imgSrc;
    String url;

    ThumbnailedFeedDiscoveryImageResponse(
            String title,
            String description,
            Integer height,
            Integer width,
            String link,
            String url
    ) {
        this.title = title;
        this.description = description;
        this.height = height;
        this.width = width;
        this.link = link;
        this.url = url;
    }

    public static ThumbnailedFeedDiscoveryImageResponse from(FeedDiscoveryImageInfo imageInfo, byte[] image) {
        ThumbnailedFeedDiscoveryImageResponse r = new ThumbnailedFeedDiscoveryImageResponse(
                imageInfo.getTitle(),
                imageInfo.getDescription(),
                imageInfo.getHeight(),
                imageInfo.getWidth(),
                imageInfo.getLink(),
                imageInfo.getUrl()
        );
        r.imgSrc = encodeBase64String(image);
        return r;
    }
}
