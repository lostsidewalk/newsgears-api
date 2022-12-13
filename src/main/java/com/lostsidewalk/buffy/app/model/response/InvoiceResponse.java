package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

@Data
public class InvoiceResponse {

    Long amountDue;
    Long amountPaid;
    Long amountRemaining;
    Long created;
    String customerEmail;
    String customerName;
    Long endingBalance;
    String hostedUrl;
    Boolean isPaid;
    String status;
    Long total;
    Long periodEnd;
    Long periodStart;
    String productDescription;

    private InvoiceResponse(
            Long amountDue,
            Long amountPaid,
            Long amountRemaining,
            Long created,
            String customerEmail,
            String customerName,
            Long endingBalance,
            String hostedUrl,
            Boolean isPaid,
            String status,
            Long total,
            Long periodEnd,
            Long periodStart,
            String productDescription)
    {
        this.amountDue = amountDue;
        this.amountPaid = amountPaid;
        this.amountRemaining = amountRemaining;
        this.created = created;
        this.customerEmail = customerEmail;
        this.customerName = customerName;
        this.endingBalance = endingBalance;
        this.hostedUrl = hostedUrl;
        this.isPaid = isPaid;
        this.status = status;
        this.total = total;
        this.periodEnd = periodEnd;
        this.periodStart = periodStart;
        this.productDescription = productDescription;
    }

    public static InvoiceResponse from(Long amountDue,
                                       Long amountPaid,
                                       Long amountRemaining,
                                       Long created,
                                       String customerEmail,
                                       String customerName,
                                       Long endingBalance,
                                       String hostedUrl,
                                       Boolean isPaid,
                                       String status,
                                       Long total,
                                       Long periodEnd,
                                       Long periodStart,
                                       String productDescription) {
        return new InvoiceResponse(
                amountDue,
                amountPaid,
                amountRemaining,
                created,
                customerEmail,
                customerName,
                endingBalance,
                hostedUrl,
                isPaid,
                status,
                total,
                periodEnd,
                periodStart,
                productDescription);
    }
}
