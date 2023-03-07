package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.app.order.StripeOrderService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.BlockingQueue;

import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RestController
public class OrderController {

    @Autowired
    private StripeOrderService stripeOrderService;

    @Autowired
    private BlockingQueue<Event> stripeCallbackQueue;
    //
    // stripe callbacks (webhooks)
    //
    private static final String STRIPE_SIGNATURE_HEADER = "Stripe-Signature";

    @PostMapping("/stripe")
    public void stripeCallback(@RequestBody String payload, HttpServletRequest request, HttpServletResponse response) throws SignatureVerificationException {
        String sigHeader = request.getHeader(STRIPE_SIGNATURE_HEADER);
        stripeCallbackQueue.add(stripeOrderService.constructEvent(sigHeader, payload));
        response.setStatus(OK.value());
    }
}
