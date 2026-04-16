package com.game.npat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.endpoint}")
    private String websocketEndpoint;

    @Value("${websocket.broker}")
    private String broker;

    @Value("${websocket.app-destination-prefix}")
    private String appDestinationPrefix;

    @Value("${cors.allowed.origins}")
    private String[] allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker
        config.enableSimpleBroker(broker);
        // Set the prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes(appDestinationPrefix);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the WebSocket endpoint with CORS support
        registry.addEndpoint(websocketEndpoint)
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }
}
