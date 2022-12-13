package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class FeedDiscoverySampleItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1209382540983644L;

    String title;
    String uri;
    String link;
    Date updateDate;

    private FeedDiscoverySampleItem(String title, String uri, String link, Date updateDate) {
        this.title = title;
        this.uri = uri;
        this.link = link;
        this.updateDate = updateDate;
    }

    public static FeedDiscoverySampleItem from(String title, String uri, String link, Date updateDate) {
        return new FeedDiscoverySampleItem(title, uri, link, updateDate);
    }
}
