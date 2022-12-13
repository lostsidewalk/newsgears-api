package com.lostsidewalk.buffy.app.user;

import java.util.Map;

abstract class OAuth2UserInfo {
    final Map<String, Object> attributes;

    OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    abstract String getId();

    abstract String getName();

    abstract String getEmail();

    abstract String getImageUrl();
}
