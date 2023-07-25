package com.lostsidewalk.buffy.app.broker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.token.TokenService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketHttpHeaders;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.lostsidewalk.buffy.app.auth.HashingUtils.sha256;
import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
public class BrokerService {

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
    public BrokerService() {
        this.sessionHandler = new CustomStompSessionHandler();
    }

    @PostConstruct
    void postConstruct() {
        log.info("Broker connection to broker initializing, brokerUrl={}", brokerUrl);
        this.connect();
    }

    void connect() {
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient socksJsClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(socksJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        try {
            WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
            handshakeHeaders.add("Authorization", "Bearer " + buildToken());
            handshakeHeaders.add("X-FeedGears", "api");
            CompletableFuture<StompSession> sessionFuture = stompClient.connectAsync(brokerUrl, handshakeHeaders, sessionHandler);
            stompSession = sessionFuture.get();
            log.info("Established connection to broker, brokerUrl={}, stompSession?={}", brokerUrl, (stompSession != null));
            subscribeToFeedgearsTopic();
        } catch (Throwable e) {
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
                        // acknowledge the message w/the broker
                        stompSession.acknowledge(messageId, true);
                        log.debug("Acknowledged messageId={} for username={}", messageId, responseUsername);
                        // send a response to the originating user
                        stompSession.send(responseDestination, "Response from API server for username=" + responseUsername + ", messageId=" + messageId);
                        log.debug("Sent work complete response for messageId={}, username={}, responseDestination={}", messageId, responseUsername, responseDestination);
                    }
                }

                private static String getResponseUsername(JsonObject payloadObj) {
                    return payloadObj.has("responseDestination") ? payloadObj.get("responseDestination").getAsString() : EMPTY;
                }

                private static String getResponseDestination(JsonObject payloadObj) {
                    return payloadObj.has("responseDestination") ? payloadObj.get("responseDestination").getAsString() : EMPTY;
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
            BrokerService.this.stompSession = session;
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
