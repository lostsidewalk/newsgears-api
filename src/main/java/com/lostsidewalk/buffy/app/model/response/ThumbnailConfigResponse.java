package com.lostsidewalk.buffy.app.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Data
@JsonInclude(NON_EMPTY)
public class ThumbnailConfigResponse {

    String imgSrc;

    List<String> errors;

    ThumbnailConfigResponse(List<String> errors) {
        this.errors = errors;
    }

    public static ThumbnailConfigResponse from(String encoded, List<String> errors) {
        ThumbnailConfigResponse t = new ThumbnailConfigResponse(errors);
        t.imgSrc = encoded;

        return t;
    }

    public static ThumbnailConfigResponse from(String encoded) {
        ThumbnailConfigResponse t = new ThumbnailConfigResponse(null);
        t.imgSrc = encoded;

        return t;
    }
}
