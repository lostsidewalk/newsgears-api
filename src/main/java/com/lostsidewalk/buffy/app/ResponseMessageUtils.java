package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.app.model.response.ResponseMessage;

class ResponseMessageUtils {

    static ResponseMessage buildResponseMessage(Object body) {
        return new ResponseMessage(body);
    }
}
