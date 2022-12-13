package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class FeedDiscoveryImageInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1201230982540983644L;

    String title;
    String description;
    Integer height;
    Integer width;
    String link;
    String transportIdent;
    String url;

    private FeedDiscoveryImageInfo(String title, String description, Integer height, Integer width, String link, String transportIdent, String url) {
        this.title = title;
        this.description = description;
        this.height = height;
        this.width = width;
        this.link = link;
        this.transportIdent = transportIdent;
        this.url = url;
    }

    public static FeedDiscoveryImageInfo from(String title, String description, Integer height, Integer width, String link, String transportIdent, String url) {
        return new FeedDiscoveryImageInfo(
                title,
                description,
                height,
                width,
                link,
                transportIdent,
                url);
    }
}
