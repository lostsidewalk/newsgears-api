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
    @NotNull
    Long id;

    @NotBlank
    @URL
    @Size(max=2048)
    String feedUrl;

    String feedTitle;

    String feedImageUrl;

    String username;

    String password;
}
