package com.lostsidewalk.buffy.app.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@AllArgsConstructor
public class RssAtomUrl {
    @NotNull
    Long id;

    @NotBlank
    @Size(max=512)
    String feedUrl;

    String feedTitle;

    String feedImageUrl;
}
