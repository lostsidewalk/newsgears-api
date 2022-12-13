package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

@Data
public class LoginResponse {
    private String authToken;
    private String username;
    private boolean hasSubscription;

    private LoginResponse(String authToken, String username, boolean hasSubscription) {
        this.authToken = authToken;
        this.username = username;
        this.hasSubscription = hasSubscription;
    }

    public static LoginResponse from(String authToken, String username, boolean hasSubscription) {
        return new LoginResponse(authToken, username, hasSubscription);
    }
}
