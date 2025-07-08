package com.project.realtimechat.config;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.project.realtimechat.service.UserService;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
	// Creates a logger instance for this class for logging authentication events
	private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
	
	// Injects the JWT service for token validation
	@Autowired
	private JwtService jwtService;
	
	// Injects the user service to load user details after token validation
	@Autowired
	private UserService userService;
	
	/**
     * Intercepts messages before they are sent to the message channel
     * Used to authenticate WebSocket connection requests
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // Extracts STOMP headers from the incoming message
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        // Checks if this is a CONNECT frame (initial WebSocket connection request)
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Logs the connection attempt
            log.info("[{}] | WebSocket connection attempt", Instant.now());
            
            // Extracts the Authorization header from the STOMP frame
            List<String> authorization = accessor.getNativeHeader("Authorization");
            
            // Processes the Authorization header if it exists
            if (authorization != null && !authorization.isEmpty()) {
                // Removes the "Bearer " prefix to get the raw token
                String token = authorization.get(0).replace("Bearer ", "");
                
                try {
                    // Validates the JWT token
                    if (jwtService.validateToken(token)) {
                        // Extracts the username from the token
                        String username = jwtService.extractUsername(token);
                        
                        // Loads the user details based on the username from the token
                        UserDetails userDetails = userService.loadUserByUsername(username);
                        
                        // Creates an authentication object with the user details and authorities
                        UsernamePasswordAuthenticationToken auth = 
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, // Principal (user details)
                                        null, // Credentials (null as we don't need the password)
                                        userDetails.getAuthorities()); // User's granted authorities/roles
                        
                        // Sets the authentication object in the WebSocket session
                        accessor.setUser(auth);
                        
                        // Logs successful authentication
                        log.info("[{}] | User {} authenticated successfully via WebSocket", 
                        		Instant.now(), username);
                    }
                } catch (Exception e) {
                    // Logs authentication failures with the error message
                    log.error("[{}] | WebSocket authentication failed {}", 
                    		Instant.now(), e.getMessage());
                }
            }
        }
        
        return message;
    }
}
