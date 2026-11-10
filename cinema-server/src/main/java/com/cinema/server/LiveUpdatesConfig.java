package com.cinema.server;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class LiveUpdatesConfig implements WebSocketConfigurer {
    private final LiveUpdates updates;
    public LiveUpdatesConfig(LiveUpdates updates) {this.updates=updates;}
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(updates,"/ws/updates");
    }
}
