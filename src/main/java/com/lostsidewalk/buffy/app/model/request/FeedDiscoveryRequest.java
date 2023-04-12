package com.lostsidewalk.buffy.app.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedDiscoveryRequest {

    @NotBlank
    @Size(max=2048)
    String url;

    String username;

    String password;

    boolean isIncludeRecommendations;
}
