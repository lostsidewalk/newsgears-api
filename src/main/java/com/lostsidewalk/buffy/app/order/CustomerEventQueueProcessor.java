package com.lostsidewalk.buffy.app.order;

import com.lostsidewalk.buffy.customer.CustomerEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;

@Slf4j
@Component
public class CustomerEventQueueProcessor implements DisposableBean, Runnable {

    @Autowired
    BlockingQueue<CustomerEvent> customerEventQueue;

    private volatile boolean isEnabled = true;

    private Thread thread;

    @PostConstruct
    void postConstruct() {
        log.info("Starting customer event queue processor");
        this.thread = new Thread(this);
        this.thread.start();
    }

    @Override
    public void run() {
        while (isEnabled) {
            try {
                CustomerEvent customerEvent = customerEventQueue.take();
                log.info("Processing customer event of type={}", customerEvent.getEventType());
            } catch (InterruptedException e) {
                log.warn("Customer event queue processor thread interrupted, exiting..");
                throw new RuntimeException(e);
            }
        }
    }

    public Health health() {
        Health.Builder builder = (this.thread != null && this.thread.isAlive() && this.isEnabled) ? Health.up() : Health.down();
        return builder.build();
    }

    @Override
    public void destroy() {
        this.isEnabled = false;
    }
}
