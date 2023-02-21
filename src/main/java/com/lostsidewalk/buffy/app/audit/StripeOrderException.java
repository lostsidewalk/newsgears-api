package com.lostsidewalk.buffy.app.audit;

public class StripeOrderException extends Exception {

    public StripeOrderException(String msg) {
        super(msg);
    }
}
