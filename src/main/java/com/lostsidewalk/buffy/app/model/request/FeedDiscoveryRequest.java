package com.lostsidewalk.buffy.app.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FeedDiscoveryRequest {

    @NotBlank
    @Size(max=2048)
    String url;

    String username;

    String password;

    public FeedDiscoveryRequest(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }
}
