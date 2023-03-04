package com.lostsidewalk.buffy.app.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.post.StagingPost;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Data
@JsonInclude(NON_EMPTY)
public class ThumbnailedPostResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 98234098234L;

    final StagingPost post;

    String postImgSrc;

    ThumbnailedPostResponse(StagingPost staginPost) {
        this.post = staginPost;
    }

    public static ThumbnailedPostResponse from(StagingPost stagingPost, String postImgSrc) {
        ThumbnailedPostResponse r = new ThumbnailedPostResponse(stagingPost);
        r.postImgSrc = postImgSrc;

        return r;
    }
}
