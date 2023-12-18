package com.lostsidewalk.buffy.app.broker;

import com.google.common.collect.ImmutableMap;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.query.SubscriptionDefinitionService;
import com.lostsidewalk.buffy.app.resolution.FeedResolutionService;
import com.lostsidewalk.buffy.app.token.TokenService;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.post.PostImporter;
import com.lostsidewalk.buffy.subscription.SubscriptionDefinition;
import jakarta.annotation.PostConstruct;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.lostsidewalk.buffy.app.ResponseMessageUtils.buildResponseMessage;
import static com.lostsidewalk.buffy.app.auth.HashingUtils.sha256;
import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static java.lang.Integer.MAX_VALUE;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;

@Slf4j
@Component
public class SubscriptionCreationTaskQueueProcessor implements DisposableBean, Runnable {

    private final StompSessionHandlerAdapter sessionHandler;

    private final ScheduledExecutorService reconnectExecutor = newSingleThreadScheduledExecutor();

    private StompSession stompSession;

    @Value("${newsgears.brokerUrl}")
    String brokerUrl;

    @Value("${newsgears.brokerSubject}")
    String brokerSubject; // server name

    @Value("${newsgears.brokerClaim}")
    String brokerClaim; // randomized secret value

    @Autowired
    TokenService tokenService;

    @Autowired
    BlockingQueue<SubscriptionCreationTask> creationTaskQueue;

    @Autowired
    FeedResolutionService feedResolutionService;

    @Autowired
    SubscriptionDefinitionService subscriptionDefinitionService;

    @Autowired
    PostImporter postImporter;

    private volatile boolean isEnabled = true;

    private Thread thread;

    @Autowired
    SubscriptionCreationTaskQueueProcessor() {
        this.sessionHandler = new CustomStompSessionHandler();
    }

    @PostConstruct
    void postConstruct() {
        log.info("Initializing subscription creation task connection, brokerUrl={}", brokerUrl);
        this.connect();

        log.info("Starting subscription creation task processing thread");
        this.thread = new Thread(this);
        this.thread.start();
    }

    void connect() {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxBinaryMessageBufferSize(1024 * 1024);
        container.setDefaultMaxSessionIdleTimeout(-1);
        container.setDefaultMaxTextMessageBufferSize(1024 * 1024);
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient(container)));
        WebSocketClient webSocketClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setInboundMessageSizeLimit(MAX_VALUE);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        try {
            WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
            handshakeHeaders.add("Authorization", "Bearer " + buildToken());
            handshakeHeaders.add("X-FeedGears", "api");
            CompletableFuture<StompSession> sessionFuture = stompClient.connectAsync(brokerUrl, handshakeHeaders, sessionHandler);
            stompSession = sessionFuture.get();
            log.info("Established connection to broker, brokerUrl={}, stompSession?={}", brokerUrl, (stompSession != null));
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error connecting to broker due to: {}, brokerUrl={}", e.getMessage(), brokerUrl);
        }
    }

    private String buildToken() {
        Map<String, Object> claims = new HashMap<>();
        String serverValidationClaim = sha256(brokerClaim, defaultCharset());
        claims.put(APP_AUTH.tokenName, serverValidationClaim);
        return tokenService.generateToken(claims, brokerSubject, APP_AUTH);
    }

    @SuppressWarnings("NullableProblems")
    private class CustomStompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            SubscriptionCreationTaskQueueProcessor.this.stompSession = session;
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                                    byte[] payload, Throwable exception) {
            log.error("Broker session exception due to: {}", exception.getMessage());
            scheduleReconnect();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            log.error("Broker transport error due to: {}", exception.getMessage());
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        log.info("Scheduling reconnect to broker...");
        reconnectExecutor.schedule(this::connect, 5, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        while (isEnabled) {
            try {
                SubscriptionCreationTask creationTask = creationTaskQueue.take();
                log.info("Processing query creation task for username={}, queueId={}, partitionCt={}",
                        creationTask.getUsername(), creationTask.getQueueId(), size(creationTask.getSubscriptions()));
                ImmutableMap<String, FeedDiscoveryInfo> discoveryCache = feedResolutionService.resolveIfNecessary(creationTask.subscriptions);
                // create the queries (for this partition)
                List<SubscriptionDefinition> createdSubscriptions = subscriptionDefinitionService.addSubscriptions(
                        creationTask.username,
                        creationTask.queueId,
                        creationTask.subscriptions);
                if (isNotEmpty(createdSubscriptions)) {
                    // perform import-from-cache (again, first partition only)
                    postImporter.doImport(createdSubscriptions, ImmutableMap.copyOf(discoveryCache));
                }
                stompSession.send(creationTask.destination, buildResponseMessage("CREATED_SUBSCRIPTION_DEFINITIONS", createdSubscriptions));
            } catch (DataAccessException | DataUpdateException | DataConflictException e) {
                log.error("Unable to create query due to: {}", e.getMessage());
            } catch (InterruptedException e) {
                log.warn("Query creation task queue processor thread interrupted, exiting..");
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
