package com.lostsidewalk.buffy.app.model.error;

import lombok.Data;

import java.util.Date;

@Data
public class UpstreamErrorDetails {

    private Date timestamp;

    private String details;
    private Integer httpStatusCode;

    private String httpStatusMessage;

    private String redirectFeedUrl;

    private Integer redirectHttpStatusCode;

    private String redirectHttpStatusMessage;

    public UpstreamErrorDetails(Date timestamp, String details,
                                Integer httpStatusCode, String httpStatusMessage, String redirectUrl, Integer redirectHttpStatusCode, String redirectHttpStatusMessage) {
        this.timestamp = timestamp;
        this.details = details;
        this.httpStatusCode = httpStatusCode;
        this.httpStatusMessage = httpStatusMessage;
        this.redirectFeedUrl = redirectUrl;
        this.redirectHttpStatusCode = redirectHttpStatusCode;
        this.redirectHttpStatusMessage = redirectHttpStatusMessage;
    }
}
