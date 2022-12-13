package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

import java.util.List;

@Data
public class PostFetchResponse {

    List<ThumbnailedPostResponse> stagingPosts;

    private PostFetchResponse(List<ThumbnailedPostResponse> stagingPosts) {
        this.stagingPosts = stagingPosts;
    }

    public static PostFetchResponse from(List<ThumbnailedPostResponse> stagingPosts) {
        return new PostFetchResponse(stagingPosts);
    }
}
