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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StripeCustomerHandler {

    private static final String PAYLOAD_EMAIL = "email";

    private static final String PAYLOAD_ID = "id";

    private static final String PAYLOAD_STATUS = "status";

    private static final String PAYLOAD_CUSTOMER = "customer";

    @Autowired
    AppLogService appLogService;

    @Autowired
    UserDao userDao;

    void customerCreated(JsonObject payload) throws DataAccessException, DataUpdateException, StripeEventException {
        log.debug("Customer created, payload={}", payload);
        if (payload.has(PAYLOAD_EMAIL)) {
            JsonElement emailElem = payload.get(PAYLOAD_EMAIL);
            if (!emailElem.isJsonNull()) {
                String emailAddress = payload.get(PAYLOAD_EMAIL).getAsString();
                User user = userDao.findByEmailAddress(emailAddress);
                if (user != null) {
                    String customerId = payload.get(PAYLOAD_ID).getAsString();
                    user.setCustomerId(customerId);
                    userDao.updateCustomerId(user);
                    appLogService.logCustomerCreated(user, emailAddress, customerId);
                } else {
                    log.error("Unable to locate user by emailAddress={}", emailAddress);
                    throw new StripeEventException("Unable to locate user by emailAddress=" + emailAddress, payload);
                }
            } else {
                log.error("Create customer payload has null email address, payload={}", payload);
                throw new StripeEventException("Create customer payload has null email address", payload);
            }
        } else {
            log.error("Create customer payload is missing email address, payload={}", payload);
            throw new StripeEventException("Create customer payload is missing email address", payload);
        }
    }

    // ok
    void customerSubscriptionCreated(JsonObject payload) {
        log.debug("Customer subscription created, payload={}", payload);
    }

    void customerSubscriptionDeleted(JsonObject payload) throws DataAccessException, StripeEventException {
        log.debug("Customer subscription deleted, payload={}", payload);
        if (payload.has(PAYLOAD_CUSTOMER)) {
            String customerId = payload.get(PAYLOAD_CUSTOMER).getAsString();
            User user = userDao.findByCustomerId(customerId);
            if (user != null) {
                user.setSubscriptionStatus(null);
                user.setSubscriptionExpDate(null);
                userDao.update(user);
                appLogService.logCustomerSubscriptionDeleted(user, customerId);
            } else {
                log.error("Unable to locate customer by Id={}", customerId);
                throw new StripeEventException("Unable to locate customer by Id=" + customerId, payload);
            }
        } else {
            log.error("Customer subscription update payload is missing status, payload={}", payload);
            throw new StripeEventException("Customer subscription update payload is missing status", payload);
        }
    }

    void customerSubscriptionTrialWillEnd(JsonObject payload) {
        log.debug("Customer subscription trial will end, payload={}", payload);
    }

    void customerSubscriptionUpdated(JsonObject payload) throws DataAccessException, DataUpdateException, StripeEventException {
        log.debug("Customer subscription updated, payload={}", payload);
        if (payload.has(PAYLOAD_STATUS)) {
            String customerId = payload.get(PAYLOAD_CUSTOMER).getAsString();
            User user = userDao.findByCustomerId(customerId);
            if (user != null) {
                String subStatus = payload.get(PAYLOAD_STATUS).getAsString();
                user.setSubscriptionStatus(subStatus);
                userDao.updateSubscriptionStatus(user);
                appLogService.logCustomerSubscriptionUpdated(user, customerId, subStatus);
            } else {
                log.error("Unable to locate customer by Id={}", customerId);
                throw new StripeEventException("Unable to locate customer by Id=" + customerId, payload);
            }
        } else {
            log.error("Customer subscription update payload is missing status, payload={}", payload);
            throw new StripeEventException("Customer subscription update payload is missing status", payload);
        }
    }
}
