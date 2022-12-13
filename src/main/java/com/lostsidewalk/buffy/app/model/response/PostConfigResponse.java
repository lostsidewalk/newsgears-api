package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;

@Data
public class PostConfigResponse {

    String postImgSrc;

    PostConfigResponse() {}

    public static PostConfigResponse from(byte[] postImg) {
        PostConfigResponse r = new PostConfigResponse();
        r.postImgSrc = encodeBase64String(postImg);

        return r;
    }
}
