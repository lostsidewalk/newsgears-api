package com.lostsidewalk.buffy.app.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@AllArgsConstructor
public class Subscription {

    Long id;

    @NotBlank(message = "{rss.atom.url.error.feed-url-is-blank}")
    @URL(message = "{rss.atom.url.error.feed-url-invalid}")
    @Size(max = 2048, message = "{rss.atom.url.error.feed-url-too-long}")
    String url;

    String title;

    String imgUrl;

    String username;

    String password;
}
