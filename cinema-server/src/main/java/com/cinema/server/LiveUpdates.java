package com.cinema.server;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** Only authenticated clients receive lightweight invalidation events; no personal data is sent. */
@Component
public class LiveUpdates extends TextWebSocketHandler {
    private final AuthSessionStore auth;
    private final ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<>();
    private record Client(WebSocketSession socket, String token) {}
    public LiveUpdates(AuthSessionStore auth) { this.auth = auth; }

    @Override public void afterConnectionEstablished(WebSocketSession socket) {
        // No auth in URL/query string or logs. Wait for first AUTH frame.
    }
    @Override protected void handleTextMessage(WebSocketSession socket, TextMessage message) throws Exception {
        if (clients.containsKey(socket.getId())) return; // Server is publish-only.
        String text=message.getPayload();
        if (!text.startsWith("AUTH ") || text.length()>300) {socket.close(CloseStatus.POLICY_VIOLATION);return;}
        String token=text.substring(5);
        if (auth.get(token)==null) {socket.close(CloseStatus.POLICY_VIOLATION);return;}
        WebSocketSession wrapped=new ConcurrentWebSocketSessionDecorator(socket, 5000, 65536);
        clients.put(socket.getId(), new Client(wrapped,token));
        wrapped.sendMessage(new TextMessage("READY"));
    }
    @Override public void afterConnectionClosed(WebSocketSession socket, CloseStatus status) {
        clients.remove(socket.getId());
    }
    @Override public void handleTransportError(WebSocketSession socket, Throwable exception) {
        clients.remove(socket.getId());
        try {socket.close();} catch(IOException ignored) {}
    }
    public void changed(String topic) {
        if (!Set.of("movies","cinemas","rooms","seats","showtimes","reservations").contains(topic)) return;
        String event="CHANGED " + topic + " " + Instant.now().toEpochMilli();
        clients.forEach((id,client) -> {
            try {
                if(auth.get(client.token())==null || !client.socket().isOpen()) {
                    clients.remove(id);client.socket().close();return;
                }
                client.socket().sendMessage(new TextMessage(event));
            } catch(Exception e) {
                clients.remove(id);
                try {client.socket().close();} catch(IOException ignored) {}
            }
        });
    }
}
