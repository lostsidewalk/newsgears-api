package com.lostsidewalk.buffy.app.audit;

import com.lostsidewalk.buffy.auth.AuthProvider;

public class AuthProviderException extends Exception {

    public final String username;

    public AuthProviderException(String username, AuthProvider expected, AuthProvider actual) {
        super("User has incorrect auth provider, username=" + username + ", expected=" + expected + ", actual=" + actual);
        this.username = username;
    }
}
