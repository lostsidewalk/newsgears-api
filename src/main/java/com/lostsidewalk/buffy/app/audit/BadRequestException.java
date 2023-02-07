package com.lostsidewalk.buffy.app.audit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }

    @SuppressWarnings("unused")
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}