package com.lostsidewalk.buffy.app.order;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.customer.CustomerEvent;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.ErrorLogService;
import com.lostsidewalk.buffy.app.audit.StripeEventLogService;
import com.lostsidewalk.buffy.app.audit.StripeEventException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

@Slf4j
@Component
public class StripeCallbackQueueProcessor implements DisposableBean, Runnable {

    @Autowired
    StripeEventLogService stripeEventLogService;

    @Autowired
    ErrorLogService errorLogService;

    @Autowired
    BlockingQueue<Event> stripeCallbackQueue;

    @Autowired
    BlockingQueue<CustomerEvent> customerEventQueue;

    @Autowired
    StripeCustomerHandler stripeCustomerHandler;

    @Autowired
    StripeInvoiceHandler stripeInvoiceHandler;

    @Autowired
    StripePaymentHandler stripePaymentHandler;

    private volatile boolean isEnabled = true;

    private Thread thread;

    @PostConstruct
    void postConstruct() {
        log.info("Starting Stripe callback queue processor");
        this.thread = new Thread(this);
        this.thread.start();
    }

    @Override
    public void run() {
        while (isEnabled) {
            try {
                Event stripeEvent = stripeCallbackQueue.take();
                processEvent(stripeEvent);
                log.debug("Processing Stripe event of type={}", stripeEvent.getType());
            } catch (InterruptedException e) {
                log.warn("Stripe callback queue processor thread interrupted, exiting..");
                throw new RuntimeException(e);
            } catch (DataAccessException e) {
                log.error("Something horrible happened on the Stripe callback queue processing thread: {}", e.getMessage(), e);
                errorLogService.logDataAccessException("sys", new Date(), e);
            } catch (DataUpdateException e) {
                log.error("Something horrible happened on the Stripe callback queue processing thread: {}", e.getMessage(), e);
                errorLogService.logDataUpdateException("sys", new Date(), e);
            } catch (StripeEventException e) {
                errorLogService.logStripeEventException("sys", new Date(), e);
            }
        }
    }

    public Health health() {
        Health.Builder builder = (this.thread != null && this.thread.isAlive() && this.isEnabled) ? Health.up() : Health.down();
        return builder.build();
    }

    private static final Gson GSON = new Gson();

    private enum StripeCallback {
        CUSTOMER_CREATED("customer.created"),
        CUSTOMER_SUBSCRIPTION_CREATED("customer.subscription.created"),
        CUSTOMER_SUBSCRIPTION_DELETED("customer.subscription.deleted"),
        CUSTOMER_SUBSCRIPTION_UPDATED("customer.subscription.updated"),
        CUSTOMER_SUBSCRIPTION_TRIAL_WILL_END("customer.subscription.trial_will_end"),
        INVOICE_CREATED("invoice.created"),
        INVOICE_FINALIZED("invoice.finalized"),
        INVOICE_FINALIZATION_FAILED("invoice.finalization_failed"),
        INVOICE_PAID("invoice.paid"),
        INVOICE_PAYMENT_ACTION_REQUIRED("invoice.payment_action_required"),
        INVOICE_PAYMENT_FAILED("invoice.payment_failed"),
        INVOICE_UPCOMING("invoice.upcoming"),
        INVOICE_UPDATED("invoice.updated"),
        PAYMENT_INTENT_CREATED("payment_intent.created"),
        PAYMENT_INTENT_SUCCEEDED("payment_intent.succeeded");

        final String ident;

        StripeCallback(String ident) {
            this.ident = ident;
        }

        static final Map<String, StripeCallback> byEventType = new HashMap<>();
        static {
            for (StripeCallback s : values()) {
                byEventType.put(s.ident, s);
            }
        }

        static StripeCallback from(String eventType) {
            return byEventType.get(eventType);
        }

    }

    private void processEvent(Event event) throws DataAccessException, DataUpdateException, StripeEventException {
        // Deserialize the nested object inside the event
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject;
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            // Deserialization failed, probably due to an API version mismatch.
            // Refer to the Javadoc documentation on `EventDataObjectDeserializer` for
            // instructions on how to handle this case, or return an error here.
            log.warn("Stripe response deserialization failed; this is probably due to an API version mismatch.");
            return;
        }
        // Handle the event
        String eventType = event.getType();
        StripeCallback callback = StripeCallback.from(eventType);
        if (callback == null) {
            stripeEventLogService.debugUnhandledStripeEvent(event, stripeObject);
        } else {
            stripeEventLogService.logStripeEvent(event, stripeObject);
            this.customerEventQueue.add(CustomerEvent.from(eventType, stripeObject.toJson()));
            String payloadStr = stripeObject.toJson();
            JsonObject payload = GSON.fromJson(payloadStr, JsonObject.class);
            switch (callback) {
                case CUSTOMER_CREATED -> stripeCustomerHandler.customerCreated(payload);
                case CUSTOMER_SUBSCRIPTION_CREATED -> stripeCustomerHandler.customerSubscriptionCreated(payload);
                case CUSTOMER_SUBSCRIPTION_DELETED -> stripeCustomerHandler.customerSubscriptionDeleted(payload);
                case CUSTOMER_SUBSCRIPTION_TRIAL_WILL_END -> stripeCustomerHandler.customerSubscriptionTrialWillEnd(payload);
                case CUSTOMER_SUBSCRIPTION_UPDATED -> stripeCustomerHandler.customerSubscriptionUpdated(payload);
                case INVOICE_CREATED -> stripeInvoiceHandler.invoiceCreated(payload);
                case INVOICE_FINALIZED -> stripeInvoiceHandler.invoiceFinalized(payload);
                case INVOICE_FINALIZATION_FAILED -> stripeInvoiceHandler.invoiceFinalizationFailed(payload);
                case INVOICE_PAID -> stripeInvoiceHandler.invoicePaid(payload);
                case INVOICE_PAYMENT_ACTION_REQUIRED -> stripeInvoiceHandler.invoicePaymentActionRequired(payload);
                case INVOICE_PAYMENT_FAILED -> stripeInvoiceHandler.invoicePaymentFailed(payload);
                case INVOICE_UPCOMING -> stripeInvoiceHandler.invoiceUpcoming(payload);
                case INVOICE_UPDATED -> stripeInvoiceHandler.invoiceUpdated(payload);
                case PAYMENT_INTENT_CREATED -> stripePaymentHandler.paymentIntentCreated(payload);
                case PAYMENT_INTENT_SUCCEEDED -> stripePaymentHandler.paymentIntentSucceeded(payload);
            }
        }
    }

    @Override
    public void destroy() {
        this.isEnabled = false;
    }
}
