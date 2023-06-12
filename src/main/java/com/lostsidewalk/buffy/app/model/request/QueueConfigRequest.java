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
public class QueueConfigRequest {

    @NotBlank(message = "{feed.config.error.ident-is-blank}")
    @Size(max = 256, message = "{feed.config.error.ident-too-long}")
    String ident;

    @Size(max = 512, message = "{feed.config.error.title-too-long}")
    String title; // optional

    @Size(max = 1024, message = "{feed.config.error.description-too-long}")
    String description; // optional

    @Size(max = 512, message = "{feed.config.error.generator-too-long}")
    String generator;

    List<@Valid Subscription> subscriptions; // optional

    @Valid
    ExportConfigRequest exportConfig;

    @Size(max = 1024, message = "{feed.config.error.copyright-too-long}")
    String copyright;

    @Size(max = 16, message = "{feed.config.error.language-too-long}")
    String language;

    @Size(max = 16384, message = "{feed.config.error.img-src-too-long}")
    String imgSrc;

    public QueueConfigRequest(String ident, String title, String description, String generator,
                              List<Subscription> subscriptions,
                              ExportConfigRequest exportConfig,
                              String copyright, String language, String imgSrc)
    {
        this.ident = ident;
        this.title = title;
        this.description = description;
        this.generator = generator;
        this.subscriptions = subscriptions;
        this.exportConfig = exportConfig;
        this.copyright = copyright;
        this.language = language;
        this.imgSrc = imgSrc;
    }

    public static QueueConfigRequest from(String ident, String title, String description, String generator,
                                          List<Subscription> subscriptions,
                                          ExportConfigRequest exportConfig,
                                          String copyright, String language, String imgSrc)
    {
        return new QueueConfigRequest(
                ident,
                title,
                description,
                generator,
                subscriptions,
                exportConfig,
                copyright,
                language,
                imgSrc
        );
    }
}
