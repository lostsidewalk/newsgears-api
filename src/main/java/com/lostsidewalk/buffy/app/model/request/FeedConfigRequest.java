package com.lostsidewalk.buffy.app.model.request;

import com.lostsidewalk.buffy.newsapi.NewsApiSources;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class FeedConfigRequest {

    @NotBlank
    @Size(max=256)
    String ident;

    @NotBlank
    @Size(max=512)
    String title;

    @Size(max=1024)
    String description; // optional

    @Size(max=512)
    String generator;

    @Size(max=512)
    String newsApiV2QueryText; // optional

    @Size(max=20)
    List<NewsApiSources> newsApiV2Sources; // optional

    String newsApiV2Language; // optional

    String newsApiV2Country; // optional

    String newsApiV2Category; // optional

    @Valid
    List<RssAtomUrl> rssAtomFeedUrls; // optional

    @Valid
    ExportConfigRequest exportConfig;

    @Size(max=1024)
    String copyright;

    @Size(max=16)
    String language;

    @Size(max=16384)
    String imgSrc;

    public FeedConfigRequest(String ident, String title, String description, String generator,
                             String newsApiV2QueryText,
                             List<RssAtomUrl> rssAtomFeedUrls,
                             ExportConfigRequest exportConfig,
                             String copyright, String language, String imgSrc)
    {
        this.ident = ident;
        this.title = title;
        this.description = description;
        this.generator = generator;
        this.newsApiV2QueryText = newsApiV2QueryText;
        this.rssAtomFeedUrls = rssAtomFeedUrls;
        this.exportConfig = exportConfig;
        this.copyright = copyright;
        this.language = language;
        this.imgSrc = imgSrc;
    }

    public static FeedConfigRequest from(String ident, String title, String description, String generator,
                                         String newsApiV2QueryText,
                                         List<RssAtomUrl> rssAtomFeedUrls,
                                         ExportConfigRequest exportConfig,
                                         String copyright, String language, String imgSrc)
    {
        return new FeedConfigRequest(
                ident,
                title,
                description,
                generator,
                newsApiV2QueryText,
                rssAtomFeedUrls,
                exportConfig,
                copyright,
                language,
                imgSrc
        );
    }
}
