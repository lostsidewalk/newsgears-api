package com.lostsidewalk.buffy.app.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Enumeration;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class OptionsAuthHandler {

    public static class MissingOptionsHeaderException extends Exception {

        final Enumeration<String> headerNames;

        MissingOptionsHeaderException(Enumeration<String> headerNames) {
            super();
            this.headerNames = headerNames;
        }
    }

    public void processRequest(HttpServletRequest request) throws MissingOptionsHeaderException {
        String accessControlRequestMethod = request.getHeader("Access-Control-Request-Method");
        String accessControlRequestHeaders = request.getHeader("Access-Control-Request-Headers");

        if (isBlank(accessControlRequestMethod) || isBlank(accessControlRequestHeaders)) {
            throw new MissingOptionsHeaderException(request.getHeaderNames());
        }
    }
}
