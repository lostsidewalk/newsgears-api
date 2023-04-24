package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.auth.AuthProvider;
import com.lostsidewalk.buffy.FrameworkConfig;
import lombok.Data;

@Data
public class SettingsResponse {
    String username;
    String emailAddress;
    AuthProvider authProvider;
    String authProviderProfileImgUrl;
    String authProviderUsername;
    FrameworkConfig frameworkConfig;
    SubscriptionResponse subscription;

    private SettingsResponse(String username, String emailAddress, AuthProvider authProvider, String authProviderProfileImgUrl, String authProviderUsername, FrameworkConfig frameworkConfig) {
        this.username = username;
        this.emailAddress = emailAddress;
        this.frameworkConfig = frameworkConfig;
        this.authProvider = authProvider;
        this.authProviderUsername = authProviderUsername;
        this.authProviderProfileImgUrl = authProviderProfileImgUrl;
    }

    public static SettingsResponse from(String username, String emailAddress, AuthProvider authProvider, String authProviderProfileImgUrl, String authProviderUsername, FrameworkConfig frameworkConfig) {
        return new SettingsResponse(username, emailAddress, authProvider, authProviderProfileImgUrl, authProviderUsername, frameworkConfig);
    }
}
