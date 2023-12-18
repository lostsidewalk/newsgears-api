package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.app.model.response.ResponseMessage;

public class ResponseMessageUtils {

    public static ResponseMessage buildResponseMessage(String responseType, Object body) {
        return new ResponseMessage(responseType, body);
    }

    static ResponseMessage buildResponseMessage(Object body) {
        return new ResponseMessage(body);
    }
}
