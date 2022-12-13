package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

@Data
public class FeedToggleResponse {
    long id;
    boolean isActive;
    String message;
}
