package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

@Data
public class SubscriptionResponse {
    Boolean cancelAtPeriodEnd;
    Long created;
    Long currentPeriodEnd;
    Long currentPeriodStart;
    Long endedAt;
    Long startDate;
    String status;
    InvoiceResponse lastInvoice;

    private SubscriptionResponse(Boolean cancelAtPeriodEnd, Long created, Long currentPeriodEnd, Long currentPeriodStart, Long endedAt,
                                 Long startDate, String status, InvoiceResponse lastInvoice) {
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
        this.created = created;
        this.currentPeriodEnd = currentPeriodEnd;
        this.currentPeriodStart = currentPeriodStart;
        this.endedAt = endedAt;
        this.startDate = startDate;
        this.status = status;
        this.lastInvoice = lastInvoice;
    }

    public static SubscriptionResponse from(Boolean cancelAtPeriodEnd, Long created, Long currentPeriodEnd, Long currentPeriodStart, Long endedAt,
                                            Long startDate, String status, InvoiceResponse lastInvoice)
    {
        return new SubscriptionResponse(
                cancelAtPeriodEnd,
                created,
                currentPeriodEnd,
                currentPeriodStart,
                endedAt,
                startDate,
                status,
                lastInvoice);
    }
}
