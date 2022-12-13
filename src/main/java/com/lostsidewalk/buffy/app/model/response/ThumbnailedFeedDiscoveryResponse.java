package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ThumbnailedFeedDiscoveryResponse {

    String title;
    String description;
    String feedType;
    String author;
    String copyright;
    String docs;
    String encoding;
    String generator;
    ThumbnailedFeedDiscoveryImageResponse image;
    ThumbnailedFeedDiscoveryImageResponse icon;
    String language;
    String link;
    String managingEditor;
    Date publishedDate;
    String styleSheet;
    List<String> supportedTypes;
    String webMaster;
    String uri;
    List<FeedDiscoverySampleItem> sampleEntries;

    ThumbnailedFeedDiscoveryResponse(
            String title,
            String description,
            String feedType,
            String author,
            String copyright,
            String docs,
            String encoding,
            String generator,
            ThumbnailedFeedDiscoveryImageResponse image,
            ThumbnailedFeedDiscoveryImageResponse icon,
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

    public static ThumbnailedFeedDiscoveryResponse from(FeedDiscoveryInfo feedDiscoveryInfo,
                                          ThumbnailedFeedDiscoveryImageResponse feedImage,
                                          ThumbnailedFeedDiscoveryImageResponse feedIcon) {
        return new ThumbnailedFeedDiscoveryResponse(
                feedDiscoveryInfo.getTitle(),
                feedDiscoveryInfo.getDescription(),
                feedDiscoveryInfo.getFeedType(),
                feedDiscoveryInfo.getAuthor(),
                feedDiscoveryInfo.getCopyright(),
                feedDiscoveryInfo.getDocs(),
                feedDiscoveryInfo.getEncoding(),
                feedDiscoveryInfo.getGenerator(),
                feedImage,
                feedIcon,
                feedDiscoveryInfo.getLanguage(),
                feedDiscoveryInfo.getLink(),
                feedDiscoveryInfo.getManagingEditor(),
                feedDiscoveryInfo.getPublishedDate(),
                feedDiscoveryInfo.getStyleSheet(),
                feedDiscoveryInfo.getSupportedTypes(),
                feedDiscoveryInfo.getWebMaster(),
                feedDiscoveryInfo.getUri(),
                feedDiscoveryInfo.getSampleEntries()
        );
    }
}
