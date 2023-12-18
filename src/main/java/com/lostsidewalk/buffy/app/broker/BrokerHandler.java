package com.lostsidewalk.buffy.app.broker;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.LockDao;
import com.lostsidewalk.buffy.app.token.TokenService;
import jakarta.annotation.PostConstruct;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.lostsidewalk.buffy.app.ResponseMessageUtils.buildResponseMessage;
import static com.lostsidewalk.buffy.app.auth.HashingUtils.sha256;
import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static java.lang.Integer.MAX_VALUE;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
public class BrokerHandler {

    private static final Gson GSON = new Gson();

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
    LockDao lockDao;

    @Autowired
    HelloWorldHandler helloWorldHandler;

    @Autowired
    OpmlUploadHandler opmlUploadHandler;

    @Autowired
    public BrokerHandler() {
        this.sessionHandler = new CustomStompSessionHandler();
    }

    enum RequestType {
        HELLO_WORLD,
        OPML_UPLOAD,
    }

    private Map<RequestType, MessageHandler> requestHandlers = new HashMap<>();

    @PostConstruct
    void postConstruct() {
        log.info("Broker handler connection initializing, brokerUrl={}", brokerUrl);
        //
        requestHandlers.put(RequestType.OPML_UPLOAD, opmlUploadHandler);
        requestHandlers.put(RequestType.HELLO_WORLD, helloWorldHandler);
        //
        this.connect();
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
            subscribeToFeedgearsTopic();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error connecting to broker due to: {}, brokerUrl={}", e.getMessage(), brokerUrl);
        }
    }

    @SuppressWarnings("NullableProblems")
    private void subscribeToFeedgearsTopic() {
        if (stompSession != null) {
            String topicUrl = "/secured/user/queue/specific-user" + "-user" + stompSession.getSessionId();
            StompHeaders headers = new StompHeaders();
            headers.setDestination(topicUrl);
            headers.set("Authorization", "Bearer " + buildToken());
            stompSession.subscribe(headers, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return byte[].class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object rawPayload) {
                    // Handle received message
                    if (rawPayload instanceof byte[]) {
                        // decode the raw payload
                        String payloadStr = new String((byte[]) rawPayload, UTF_8);
                        String messageId = headers.getMessageId();
                        // validate the messageId and payload
                        if (isBlank(messageId)) {
                            throw new RuntimeException("STOMP frame is missing message identifier, headers=" + headers);
                        }
                        if (isBlank(payloadStr)) {
                            throw new RuntimeException("STOMP frame is missing message payload, headers=" + headers);
                        }
                        log.debug("Received message: messageId={}, rawPayload={}", messageId, payloadStr);
                        // get the FG header message; this contains a JSON object with the response destination to use
                        String headerMessage = headers.getFirst("message");
                        // decode the header message string into JSON
                        JsonObject headerMessageObj = GSON.fromJson(headerMessage, JsonObject.class);
                        // get the response destination from the header message
                        String responseUsername = getResponseUsername(headerMessageObj);
                        String responseDestination = getResponseDestination(headerMessageObj);
                        // check the request type
                        RequestType requestType = getRequestType(headerMessageObj);
                        // get the request handler
                        MessageHandler<?> messageHandler = requestHandlers.get(requestType);
                        if (messageHandler == null) {
                            throw new RuntimeException("Unable to handle requests of this type, requestType=" + requestType
                                    + ", responseDestination=" + responseDestination
                                    + ", responseUsername=" + responseUsername
                                    + ", headerMessageObj=" + headerMessageObj);
                        }
                        // extract the payload
                        JsonElement payload = getPayload(headerMessageObj);
                        // compute the lock values
                        String payloadHash = sha256(payload.toString(), UTF_8);
                        String lockKey = "opmlUpload_" + payloadHash;
                        // acquire the lock (prevents procesing this payload multiple times, e.g., by the front-end submitting multiple requests to the broker)
                        if (lockDao.acquireLock(lockKey, payloadHash)) {
                            log.debug("Acquired lock to proces work request, lockKey={}, payloadHash={}", lockKey, payloadHash);
                            try {
                                // process the message
                                Object responseObject = messageHandler.handleMessage(payload, responseUsername, responseDestination);
                                stompSession.send(
                                        responseDestination,
                                        buildResponseMessage(messageHandler.getResponseType(), responseObject)
                                );
                            } catch (DataAccessException | DataUpdateException | DataConflictException e) {
                                log.error("Work request for username={}, messageId={}, requestType={} failed due to: {}", responseUsername, messageId, requestType, e.getMessage());
                            } finally {
                                // release the lock
                                if (!lockDao.releaseLock(lockKey, payloadHash)) {
                                    log.warn("Unable to release lock on work request, lockKey={}, payloadHash={}", lockKey, payloadHash);
                                }
                            }
                        }
                        // acknowledge the message w/the broker
                        stompSession.acknowledge(messageId, true);
                        log.info("Acknowledged work request for username={}, requestType={}, messageId={}", responseUsername, requestType, messageId);
                    }
                }

                private static RequestType getRequestType(JsonObject headerMessageObj) {
                    if (headerMessageObj.has("requestType")) {
                        JsonElement responseRequestType = headerMessageObj.get("requestType");
                        if (!responseRequestType.isJsonNull()) {
                            String requestTypeStr = responseRequestType.getAsString();
                            for (RequestType r : RequestType.values()) {
                                if (r.name().equals(requestTypeStr)) {
                                    return r;
                                }
                            }
                        }
                    }
                    return null;
                }

                private static String getResponseUsername(JsonObject headerMessageObj) {
                    if (headerMessageObj.has("responseUsername")) {
                        JsonElement responseUsernameElem = headerMessageObj.get("responseUsername");
                        if (!responseUsernameElem.isJsonNull()) {
                            return responseUsernameElem.getAsString();
                        }
                    }
                    return EMPTY;
                }

                private static String getResponseDestination(JsonObject headerMessageObj) {
                    if (headerMessageObj.has("responseDestination")) {
                        JsonElement responseDestinationElem = headerMessageObj.get("responseDestination");
                        if (!responseDestinationElem.isJsonNull()) {
                            return responseDestinationElem.getAsString();
                        }
                    }
                    return EMPTY;
                }

                private static JsonElement getPayload(JsonObject headerMessageObj) {
                    if (headerMessageObj.has("payload")) {
                        return headerMessageObj.get("payload");
                    }
                    return null;
                }
            });
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
            BrokerHandler.this.stompSession = session;
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

    @EventListener
    public void handleSessionConnectedEvent(SessionConnectedEvent event) {
        log.info("Broker session connected: {}", event);
    }

    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        log.info("Broker session disconnected: {}", event);
    }
}
