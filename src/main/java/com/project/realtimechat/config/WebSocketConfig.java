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
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{
	private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);
	
	@Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

	/**
	 * WebSocket endpoint: ws://localhost:8080/ws
	 * SockJS fallback: http://localhost:8080/ws/websocket
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
        
        log.info("User configuring WebSocket message broker");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP Endpoints where clients will connect
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // Configure CORS
                .withSockJS() // Fallback options for browsers that don't support WebSocket
                .setStreamBytesLimit(10 * 1024 * 1024) // 10MB
                .setHttpMessageCacheSize(10000)
                .setDisconnectDelay(30 * 1000);
        
        // Add a plain WebSocket endpoint as well (without SockJS)
        // registry.addEndpoint("/ws")
        //         .setAllowedOriginPatterns("*");
        
        log.info("User registered WebSocket STOMP endpoints");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
            .setMessageSizeLimit(10 * 1024 * 1024) // 10MB
            .setSendBufferSizeLimit(10 * 1024 * 1024) // 10MB
            .setSendTimeLimit(20 * 1000); // 20 seconds
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add authentication Interceptor for incoming messages
        registration.interceptors(webSocketAuthInterceptor);
        
        // Set thread pool executor for handling messages
        registration.taskExecutor().corePoolSize(8).maxPoolSize(16).queueCapacity(100);
        
        // ADD THESE SETTINGS
        registration.taskExecutor().keepAliveSeconds(60);
//        registration.taskExecutor().allowCoreThreadTimeOut(true);
        
        log.info("Configured WebSocket client inbound channel with auth interceptor");
    }
    
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // Set thread pool executor for outbound messages
        registration.taskExecutor().corePoolSize(8).maxPoolSize(16).queueCapacity(100);
        
        registration.taskExecutor().keepAliveSeconds(60);
        
        log.info("Configured WebSocket client outbound channel");
    }
	
}
