package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class FeedDiscoveryInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 112341230982130984L;

    String title;
    String description;
    String feedType;
    String author;
    String copyright;
    String docs;
    String encoding;
    String generator;
    FeedDiscoveryImageInfo image;
    FeedDiscoveryImageInfo icon;
    String language;
    String link;
    String managingEditor;
    Date publishedDate;
    String styleSheet;
    List<String> supportedTypes;
    String webMaster;
    String uri;
    List<FeedDiscoverySampleItem> sampleEntries;

    private FeedDiscoveryInfo(
            String title,
            String description,
            String feedType,
            String author,
            String copyright,
            String docs,
            String encoding,
            String generator,
            FeedDiscoveryImageInfo image,
            FeedDiscoveryImageInfo icon,
            String language,
            String link,
            String managingEditor,
            Date publishedDate,
            String styleSheet,
            List<String> supportedTypes,
            String webMaster,
            String uri,
            List<FeedDiscoverySampleItem> sampleEntries) {
        this.title = title;
        this.description = description;
        this.feedType = feedType;
        this.author = author;
        this.copyright = copyright;
        this.docs = docs;
        this.encoding = encoding;
        this.generator = generator;
        this.image = image;
        this.icon = icon;
        this.language = language;
        this.link = link;
        this.managingEditor = managingEditor;
        this.publishedDate = publishedDate;
        this.styleSheet = styleSheet;
        this.supportedTypes = supportedTypes;
        this.webMaster = webMaster;
        this.uri = uri;
        this.sampleEntries = sampleEntries;
    }

    public static FeedDiscoveryInfo from(String title,
                                         String description,
                                         String feedType,
                                         String author,
                                         String copyright,
                                         String docs,
                                         String encoding,
                                         String generator,
                                         FeedDiscoveryImageInfo image,
                                         FeedDiscoveryImageInfo icon,
                                         String language,
                                         String link,
                                         String managingEditor,
                                         Date publishedDate,
                                         String styleSheet,
                                         List<String> supportedTypes,
                                         String webMaster,
                                         String uri,
                                         List<FeedDiscoverySampleItem> sampleEntries) {
        return new FeedDiscoveryInfo(
                title,
                description,
                feedType,
                author,
                copyright,
                docs,
                encoding,
                generator,
                image,
                icon,
                language,
                link,
                managingEditor,
                publishedDate,
                styleSheet,
                supportedTypes,
                webMaster,
                uri,
                sampleEntries
        );
    }
}
