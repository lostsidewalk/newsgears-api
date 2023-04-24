package com.lostsidewalk.buffy.app.user;

import java.util.Map;

import static com.lostsidewalk.buffy.auth.AuthProvider.GITHUB;
import static com.lostsidewalk.buffy.auth.AuthProvider.GOOGLE;

class OAuth2UserInfoFactory {

    static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if(registrationId.equalsIgnoreCase(GOOGLE.toString())) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(GITHUB.toString())) {
            return new GithubOAuth2UserInfo(attributes);
        } else {
            throw new OAuth2AuthenticationProcessingException("Sorry! Login with " + registrationId + " is not supported yet.");
        }
    }
}
