package com.lostsidewalk.buffy.app.model;

public class AppToken {

    public final String authToken;

    public final int maxAgeInSeconds;

    public AppToken(String authToken, int maxAgeInSeconds) {
        this.authToken = authToken;
        this.maxAgeInSeconds = maxAgeInSeconds;
    }
}
