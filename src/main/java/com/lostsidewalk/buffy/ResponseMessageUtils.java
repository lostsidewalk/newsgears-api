package com.lostsidewalk.buffy;

class ResponseMessageUtils {

    static PostController.ResponseMessage buildResponseMessage(String body) {
        return new PostController.ResponseMessage(body);
    }
}
