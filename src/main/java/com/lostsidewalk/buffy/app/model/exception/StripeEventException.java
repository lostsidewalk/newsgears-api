package com.lostsidewalk.buffy.app.model.exception;

import com.google.gson.JsonObject;

public class StripeEventException extends Exception {

    public final JsonObject eventPayload;

    public StripeEventException(String msg, JsonObject eventPayload) {
        super(msg);
        this.eventPayload = eventPayload;
    }
}
