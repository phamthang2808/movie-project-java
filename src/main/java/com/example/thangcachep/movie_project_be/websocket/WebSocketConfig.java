package com.example.thangcachep.movie_project_be.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final WatchTogetherHandler watchTogetherHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(watchTogetherHandler, "/ws/watch-together")
                .setAllowedOrigins("*"); // Configure properly in production
        // Note: Add .withSockJS() if you want SockJS fallback support
        // You'll need to add spring-boot-starter-websocket dependency which includes SockJS
    }
}

