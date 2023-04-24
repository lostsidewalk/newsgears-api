package com.lostsidewalk.buffy.app.order;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.auth.User;
import com.lostsidewalk.buffy.auth.UserDao;
import com.lostsidewalk.buffy.app.model.response.InvoiceResponse;
import com.lostsidewalk.buffy.app.model.response.StripeResponse;
import com.lostsidewalk.buffy.app.model.response.SubscriptionResponse;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionCollection;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION;
import static com.stripe.param.checkout.SessionCreateParams.PaymentMethodType.CARD;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@Transactional
public class StripeOrderService {


    @Autowired
    UserDao userDao;

    @Value("${stripe.price.id}")
    private String stripePriceId;

    @Value("${stripe.order.success.url}")
    private String orderSuccessUrl;

    @Value("${stripe.order.cancel.url}")
    private String orderCancelUrl;

    @Value("${stripe.secret.key}")
    private String apiKey;

    @Value("${stripe.wh.secret.key}")
    private String whApiKey;

    @PostConstruct
    void postConstruct() {
        // set the private keys
        log.info("Stripe order service initialized");
        log.debug("Stripe order service initialized with API key={}, WH API key={}", apiKey, whApiKey);
        Stripe.apiKey = apiKey;
    }

    public StripeResponse createCheckoutSession(String username) throws StripeException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        String emailAddress = user.getEmailAddress();
        String customerId = user.getCustomerId();
        List<SessionCreateParams.LineItem> sessionItemsList = new ArrayList<>();
        sessionItemsList.add(createSessionLineItem());

        // build the session param
        // Note: we may only specify one of these parameters: customer, customer_email
        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(CARD)
                .setMode(SUBSCRIPTION) // ok
                .setCancelUrl(orderCancelUrl) // ok
                .addAllLineItem(sessionItemsList) // ok
                .setSuccessUrl(orderSuccessUrl) // ok
                .setCustomer(customerId) // ok
                .setCustomerEmail(customerId == null ? emailAddress : null) // ok
                .setClientReferenceId(username) // ok
                .build();
        //
        Session session = Session.create(params);

        log.info("Created checkout session for username={}, sessionId={}, sessionCustomerId={}, sessionCustomerEmail={}",
                username, session.getId(), session.getCustomer(), session.getCustomerEmail());

        return StripeResponse.from(session.getId(), session.getUrl());
    }

    private SessionCreateParams.LineItem createSessionLineItem() {
        return new SessionCreateParams.LineItem.Builder()
                .setPrice(this.stripePriceId)
                .setQuantity(1L)
                .build();
    }

    public Event constructEvent(String sigHeader, String eventStr) throws SignatureVerificationException {
        return Webhook.constructEvent(eventStr, sigHeader, whApiKey);
    }

    public List<SubscriptionResponse> getSubscriptions(String username) throws StripeException, DataAccessException {
        return _getSubscriptions(username).stream()
                .map(s -> SubscriptionResponse.from(
                        s.getCancelAtPeriodEnd(),
                        s.getCreated(),
                        s.getCurrentPeriodEnd(),
                        s.getCurrentPeriodStart(),
                        s.getEndedAt(),
                        s.getStartDate(),
                        s.getStatus(), buildInvoiceResponse(s.getLatestInvoiceObject())))
                .collect(toList());
    }

    private List<Subscription> _getSubscriptions(String username) throws StripeException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        String customerId = user.getCustomerId();
        if (customerId == null) {
            return List.of();
        }
        SubscriptionCollection subscriptionCollection = Subscription.list(
                SubscriptionListParams.builder()
                        .addAllExpand(Collections.singletonList("data.latest_invoice"))
                        .setCustomer(customerId)
                        .build());
        List<Subscription> subscriptions = new ArrayList<>();
        if (subscriptionCollection != null) {
            subscriptionCollection.autoPagingIterable().forEach(subscriptions::add);
        }

        return subscriptions;
    }

    private static InvoiceResponse buildInvoiceResponse(Invoice invoice) {
        return invoice == null ? null : InvoiceResponse.from(
                invoice.getAmountDue(),
                invoice.getAmountPaid(),
                invoice.getAmountRemaining(),
                invoice.getCreated(),
                invoice.getCustomerEmail(),
                invoice.getCustomerName(),
                invoice.getEndingBalance(),
                invoice.getHostedInvoiceUrl(),
                invoice.getPaid(),
                invoice.getStatus(),
                invoice.getTotal(),
                invoice.getPeriodEnd(),
                invoice.getPeriodStart(),
                invoice.getDescription());
    }

    public void cancelSubscription(String username) throws StripeException, DataAccessException {
        List<Subscription> subscriptions = _getSubscriptions(username);
        SubscriptionUpdateParams updateParams = new SubscriptionUpdateParams.Builder().setCancelAtPeriodEnd(true).build();
        for (Subscription subscription : subscriptions) {
            subscription.update(updateParams);
        }
    }

    public void resumeSubscription(String username) throws StripeException, DataAccessException {
        List<Subscription> subscriptions = _getSubscriptions(username);
        SubscriptionUpdateParams updateParams = new SubscriptionUpdateParams.Builder().setCancelAtPeriodEnd(false).build();
        for (Subscription subscription : subscriptions) {
            subscription.update(updateParams);
        }
    }
}
