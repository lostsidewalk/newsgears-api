package com.lostsidewalk.buffy.app.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@AllArgsConstructor
public class RssAtomUrl {

    @NotNull(message = "{rss.atom.url.error.id-is-null}")
    Long id;

    @NotBlank(message = "{rss.atom.url.error.feed-url-is-blank}")
    @URL(message = "{rss.atom.url.error.feed-url-invalid}")
    @Size(max = 2048, message = "{rss.atom.url.error.feed-url-too-long}")
    String feedUrl;

    String feedTitle;

    String feedImageUrl;

    String username;

    String password;
}
