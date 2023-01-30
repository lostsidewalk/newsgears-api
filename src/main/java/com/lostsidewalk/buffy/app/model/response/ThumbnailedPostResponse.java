package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.post.StagingPost;
import lombok.Data;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;

@Data
public class ThumbnailedPostResponse {

    final StagingPost post;

    String postImgSrc;

    ThumbnailedPostResponse(StagingPost staginPost) {
        this.post = staginPost;
    }

    public static ThumbnailedPostResponse from(StagingPost stagingPost, byte[] postImg) {
        ThumbnailedPostResponse r = new ThumbnailedPostResponse(stagingPost);
        r.postImgSrc = encodeBase64String(postImg);

        return r;
    }
}
