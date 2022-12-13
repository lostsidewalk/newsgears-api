package com.lostsidewalk.buffy.app.auth;

import jakarta.servlet.http.Cookie;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class CookieBuilder {

    private final String name;
    private final String value;

    public CookieBuilder(String name, String value) {
        this.name = name;
        this.value = value;
    }

    boolean isHttpOnly = false;
    boolean isSecure = false;
    int maxAge = Integer.MAX_VALUE;
    String path = "/";
    String domain = EMPTY;

    @SuppressWarnings("unused")
    public CookieBuilder setHttpOnly(boolean httpOnly) {
        isHttpOnly = httpOnly;
        return this;
    }

    @SuppressWarnings("unused")
    public CookieBuilder setSecure(boolean secure) {
        isSecure = secure;
        return this;
    }

    @SuppressWarnings("unused")
    public CookieBuilder setMaxAge(int maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    @SuppressWarnings("unused")
    public CookieBuilder setPath(String path) {
        this.path = path;
        return this;
    }

    @SuppressWarnings("unused")
    public CookieBuilder setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public Cookie build() {
        Cookie c = new Cookie(this.name, this.value);
        c.setHttpOnly(this.isHttpOnly);
        c.setSecure(this.isSecure); // true for prod/https
        c.setMaxAge(this.maxAge);
        c.setPath(this.path);
        c.setDomain(this.domain);

        return c;
    }
}
