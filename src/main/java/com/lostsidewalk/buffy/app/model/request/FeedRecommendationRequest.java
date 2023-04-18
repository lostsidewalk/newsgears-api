package com.lostsidewalk.buffy.app.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class FeedRecommendationRequest {

    @NotBlank(message = "{feed.recommendation.error.url-is-blank}")
    @Size(max = 2048, message = "{feed.recommendation.error.url-too-long}")
    String url;

    public FeedRecommendationRequest(String url) {
        this.url = url;
    }
}
