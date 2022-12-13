package com.lostsidewalk.buffy.app.model.request;

import lombok.Data;

import jakarta.validation.constraints.Size;
import java.util.Date;

@Data
public class PostUpdateRequest {
    @Size(max=256)
    String sourceName;
    @Size(max=1024)
    String sourceUrl;
    @Size(max=1024)
    String postTitle;
    @Size(max=1024)
    String postDesc;
    @Size(max=1024)
    String postUrl;
    @Size(max=1024)
    String postImgUrl;
    @Size(max=16384)
    String postImgSrc;
    @Size(max=2048)
    String postComment;
    @Size(max=1024)
    String postRights;
    @Size(max=1024)
    String xmlBase;
    @Size(max=256)
    String contributorName;
    @Size(max=512)
    String contributorEmail;
    @Size(max=256)
    String authorName;
    @Size(max=512)
    String authorEmail;
    @Size(max=256)
    String postCategory;
    Date expirationTimestamp;
    @Size(max=1024)
    String enclosureUrl;
}
