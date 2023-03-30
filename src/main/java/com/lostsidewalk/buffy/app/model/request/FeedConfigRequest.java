package com.lostsidewalk.buffy.app.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

@Data
@NoArgsConstructor
@JsonInclude(NON_ABSENT)
public class FeedConfigRequest {

    @NotBlank
    @Size(max=256)
    String ident;

    @Size(max=512)
    String title; // optional

    @Size(max=1024)
    String description; // optional

    @Size(max=512)
    String generator;

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
                             List<RssAtomUrl> rssAtomFeedUrls,
                             ExportConfigRequest exportConfig,
                             String copyright, String language, String imgSrc)
    {
        this.ident = ident;
        this.title = title;
        this.description = description;
        this.generator = generator;
        this.rssAtomFeedUrls = rssAtomFeedUrls;
        this.exportConfig = exportConfig;
        this.copyright = copyright;
        this.language = language;
        this.imgSrc = imgSrc;
    }

    public static FeedConfigRequest from(String ident, String title, String description, String generator,
                                         List<RssAtomUrl> rssAtomFeedUrls,
                                         ExportConfigRequest exportConfig,
                                         String copyright, String language, String imgSrc)
    {
        return new FeedConfigRequest(
                ident,
                title,
                description,
                generator,
                rssAtomFeedUrls,
                exportConfig,
                copyright,
                language,
                imgSrc
        );
    }
}
