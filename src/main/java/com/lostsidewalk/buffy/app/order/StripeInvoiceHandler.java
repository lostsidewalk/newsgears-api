package com.lostsidewalk.buffy.app.order;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.auth.User;
import com.lostsidewalk.buffy.auth.UserDao;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.audit.StripeEventException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import static java.util.Calendar.DATE;

@Slf4j
@Component
public class StripeInvoiceHandler {

    private static final String PAYLOAD_STATUS = "status";

    private static final String PAYLOAD_STATUS_PAID = "paid";

    private static final String PAYLOAD_CUSTOMER_EMAIL = "customer_email";

    @Autowired
    AppLogService appLogService;

    @Autowired
    UserDao userDao;

    // ok
    void invoiceCreated(JsonObject payload) {
        log.debug("Invoice created, payload={}", payload);
    }

    // ok
    void invoiceFinalized(JsonObject payload) {
        log.debug("Invoice finalized, payload={}", payload);
    }

    void invoiceFinalizationFailed(JsonObject payload) {
        log.debug("Invoice finalization failed, payload={}", payload);
    }

    void invoicePaid(JsonObject payload) throws DataAccessException, DataUpdateException, StripeEventException {
        log.debug("Invoice paid, payload={}", payload);
        if (payload.has(PAYLOAD_STATUS)) {
            String status = payload.get(PAYLOAD_STATUS).getAsString();
            if (StringUtils.equals(status, PAYLOAD_STATUS_PAID)) {
                if (payload.has(PAYLOAD_CUSTOMER_EMAIL)) {
                    JsonElement emailElem = payload.get(PAYLOAD_CUSTOMER_EMAIL);
                    if (!emailElem.isJsonNull()) {
                        String emailAddress = emailElem.getAsString();
                        User user = userDao.findByEmailAddress(emailAddress);
                        if (user != null) {
                            add30DaysToSubscription(user);
                            userDao.updateSubscriptionExpDate(user);
                            appLogService.logInvoicePaid(user, emailAddress);
                        } else {
                            log.error("Unable to locate user by emailAddress={}", emailAddress);
                            throw new StripeEventException("Unable to locate user by emailAddress=" + emailAddress, payload);
                        }
                    } else {
                        log.error("Invoice paid payload is missing email address, payload={}", payload);
                        throw new StripeEventException("Invoice paid payload is missing email address", payload);
                    }
                } else {
                    log.error("Invoice paid payload is missing email address, payload={}", payload);
                    throw new StripeEventException("Invoice paid payload is missing email address", payload);
                }
            } else {
                log.error("Invoice paid payload has unexpected status: {}", payload);
                throw new StripeEventException("Invoice paid payload has unexpected status={}", payload);
            }
        } else {
            log.error("Invoice paid payload is missing status, payload={}", payload);
            throw new StripeEventException("Invoice paid payload is missing status", payload);
        }
    }

    void add30DaysToSubscription(User user) {
        Date currentExpDate = Optional.ofNullable(user.getSubscriptionExpDate()).orElse(new Date());
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentExpDate);
        cal.add(DATE, 33);
        Date newExpDate = cal.getTime();
        user.setSubscriptionExpDate(newExpDate);
    }

    // not sure
    void invoicePaymentActionRequired(JsonObject payload) {
        log.debug("Invoice payment action required, payload={}", payload);
    }

    // not sure
    void invoicePaymentFailed(JsonObject payload) {
        log.debug("Invoice payment failed, payload={}", payload);
    }

    // ok
    void invoiceUpcoming(JsonObject payload) {
        log.debug("Invoice upcoming, payload={}", payload);
    }

    // not sure
    void invoiceUpdated(JsonObject payload) {
        log.debug("Invoice updated, payload={}", payload);
    }
}
