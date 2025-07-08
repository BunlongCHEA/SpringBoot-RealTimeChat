package com.project.realtimechat.config;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{
	private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);
	
	@Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

	/**
	 * http://localhost:8080/ws/websocket
	 */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory broker for message broadcasting
        // Client can subscribe to these destinations to receive messages
        config.enableSimpleBroker("/topic", "/queue", "/user");
        
        // Define the prefix for messages that are bound for methods annotated with @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
        
        // Configure user destination prefix for private messages
        config.setUserDestinationPrefix("/user");
        
        log.info("[{}] | User configuring WebSocket message broker", Instant.now());
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP Endpoints where clients will connect
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // Configure CORS
                .withSockJS(); // Fallback options for browsers that don't support WebSocket
        
        // Add a plain WebSocket endpoint as well (without SockJS)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
        
        log.info("[{}] | User registered WebSocket STOMP endpoints", Instant.now());
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add authentication Interceptor
        registration.interceptors(webSocketAuthInterceptor);
        
        log.info("[{}] | User configured WebSocket client inbound channel with auth interceptor", Instant.now());
    }
	
}
