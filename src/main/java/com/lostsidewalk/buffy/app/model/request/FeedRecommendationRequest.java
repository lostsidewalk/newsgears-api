package com.lostsidewalk.buffy.app.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class FeedRecommendationRequest {

    @NotBlank
    @Size(max=2048)
    String url;

    public FeedRecommendationRequest(String url) {
        this.url = url;
    }
}
