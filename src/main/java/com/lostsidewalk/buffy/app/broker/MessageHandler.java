package com.lostsidewalk.buffy.app.broker;

import com.google.gson.JsonElement;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;

@FunctionalInterface
public interface MessageHandler<RESPONSE_TYPE> {

    RESPONSE_TYPE handleMessage(JsonElement payload, String username, String destination)
            throws DataConflictException, DataAccessException, DataUpdateException;

    default String getResponseType() {
        return "MESSAGE";
    }
}
