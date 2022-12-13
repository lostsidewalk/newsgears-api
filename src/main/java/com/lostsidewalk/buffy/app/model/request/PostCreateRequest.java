package com.lostsidewalk.buffy.app.model.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Date;

@Data
public class PostCreateRequest {
    @NotBlank
    @Size(max=256)
    String feedIdent;
    @Size(max=256)
    String sourceName;
    @Size(max=1024)
    String sourceUrl;
    @NotBlank
    @Size(max=1024)
    String title;
    @Size(max=1024)
    String description;
    @NotBlank
    @Size(max=1024)
    String url;
    @Size(max=1024)
    String imgUrl;
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
