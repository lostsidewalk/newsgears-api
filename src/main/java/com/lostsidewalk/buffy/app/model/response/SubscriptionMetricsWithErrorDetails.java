package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.subscription.SubscriptionMetrics;
import lombok.Data;

import java.util.Date;

@Data
public class SubscriptionMetricsWithErrorDetails {

    private Long subscriptionId;
    private Integer httpStatusCode;
    private String httpStatusMessage;
    private String redirectFeedUrl;
    private Integer redirectHttpStatusCode;
    private String redirectHttpStatusMessage;
    private Integer importCt;
    private Integer persistCt;
    private Integer archiveCt;
    private Date importTimestamp;
    private String queryExceptionTypeMessage;

    public SubscriptionMetricsWithErrorDetails(Long subscriptionId, Integer httpStatusCode, String httpStatusMessage, String redirectFeedUrl, Integer redirectHttpStatusCode, String redirectHttpStatusMessage, Integer importCt, Integer persistCt, Integer archiveCt, Date importTimestamp, String queryExceptionTypeMessage) {
        this.subscriptionId = subscriptionId;
        this.httpStatusCode = httpStatusCode;
        this.httpStatusMessage = httpStatusMessage;
        this.redirectFeedUrl = redirectFeedUrl;
        this.redirectHttpStatusCode = redirectHttpStatusCode;
        this.redirectHttpStatusMessage = redirectHttpStatusMessage;
        this.importCt = importCt;
        this.persistCt = persistCt;
        this.archiveCt = archiveCt;
        this.importTimestamp = importTimestamp;
        this.queryExceptionTypeMessage = queryExceptionTypeMessage;
    }

    public static SubscriptionMetricsWithErrorDetails from(SubscriptionMetrics subscriptionMetrics, String errorDetails) {
        return new SubscriptionMetricsWithErrorDetails(
                subscriptionMetrics.getSubscriptionId(),
                subscriptionMetrics.getHttpStatusCode(),
                subscriptionMetrics.getHttpStatusMessage(),
                subscriptionMetrics.getRedirectFeedUrl(),
                subscriptionMetrics.getRedirectHttpStatusCode(),
                subscriptionMetrics.getRedirectHttpStatusMessage(),
                subscriptionMetrics.getImportCt(),
                subscriptionMetrics.getPersistCt(),
                subscriptionMetrics.getArchiveCt(),
                subscriptionMetrics.getImportTimestamp(),
                errorDetails);
    }
}
