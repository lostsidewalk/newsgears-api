package com.lostsidewalk.buffy.app.model.request;

import com.lostsidewalk.buffy.post.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.List;

@Data
public class PostCreateRequest {
    @NotNull
    Long feedId;
    @Size(max=256)
    String sourceName;
    @Size(max=1024)
    String sourceUrl;
    @NotBlank
    @Size(max=1024)
    ContentObject postTitle;
    @Size(max=8192)
    ContentObject postDesc;
    @Size(max=256)
    List<ContentObject> postContents;
    @Valid
    PostMedia postMedia;
    @Valid
    PostITunes postITunes;
    @NotBlank
    @Size(max=1024)
    String postUrl;
    @Size(max=256)
    List<PostUrl> postUrls;
    @Size(max=1024)
    String postImgUrl;
    @Size(max=16384)
    String postImgSrc;
    @Size(max=2048)
    String postComment;
    @Size(max=1024)
    String postRights;
    @Size(max=256)
    List<PostPerson> authors;
    @Size(max=256)
    List<PostPerson> contributors;
    @Size(max=256)
    List<String> postCategories;
    Date expirationTimestamp;
    @Size(max=256)
    List<PostEnclosure> enclosures;
}
