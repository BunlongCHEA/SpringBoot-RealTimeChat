package com.project.realtimechat.config;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.project.realtimechat.dto.ChatRoomDTO;
import com.project.realtimechat.dto.ParticipantDTO;
import com.project.realtimechat.entity.EnumStatus;
import com.project.realtimechat.entity.User;
import com.project.realtimechat.repository.UserRepository;
import com.project.realtimechat.service.ParticipantService;

@Component
public class WebSocketEventListener {
	private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
	
//	private static final String utcString = Instant.now().toString();

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;
    
    @Autowired
    private ParticipantService participantService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SimpUserRegistry userRegistry;
    
    @Autowired
    private WebSocketEventPublisher eventPublisher; 

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication auth = (Authentication) headerAccessor.getUser();
        
        if (auth != null) {
            String username = auth.getName();
            log.info("User {} connected to WebSocket", 
            		 username);
            
            // Update user online status
            try {
                Long userId = getUserIdFromUsername(username);
                if (userId != null) {
                	// Update online status using the new service method
                    participantService.updateOnlineStatus(userId, true);
                    
                    // Get user participations and broadcast status
                    List<ParticipantDTO> userParticipations = participantService
                            .getParticipantsByUserId(userId)
                            .getBody()
                            .getData();
                    
                    logActiveUsers("User connected");
                    
                    // Broadcast online status to users in same chat rooms
                    eventPublisher.broadcastUserStatusUpdate(userId, username, true, userParticipations);
                }
            } catch (Exception e) {
                log.error("Error updating user online status: {}",  e.getMessage());
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication auth = (Authentication) headerAccessor.getUser();
        
        if (auth != null) {
            String username = auth.getName();
            log.info("User {} disconnected from WebSocket", 
                     username);
            
            // Update user offline status
            try {
                Long userId = getUserIdFromUsername(username);
                if (userId != null) {
                	// Update offline status using the new service method
                    participantService.updateOnlineStatus(userId, false);
                    
                    // Get user participations and broadcast status
                    List<ParticipantDTO> userParticipations = participantService
                            .getParticipantsByUserId(userId)
                            .getBody()
                            .getData();
                    
                    logActiveUsers("User disconnected");
                    
                    // Broadcast offline status to users in same chat rooms
                    eventPublisher.broadcastUserStatusUpdate(userId, username, false, userParticipations);
                }
            } catch (Exception e) {
                log.error("Error updating user offline status: {}",  e.getMessage());
            }
        }
    }
    
    /**
     * Handles when users subscribe to channels/topics
     * This validates if they have permission to subscribe
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Authentication auth = (Authentication) headerAccessor.getUser();
        String destination = headerAccessor.getDestination();
        
        if (auth != null && destination != null) {
            String username = auth.getName();
            log.info("User {} subscribed to {}", 
                     username, destination);
            
            // Validate subscription permission
            try {
                if (destination.startsWith("/topic/chat/")) {
                    // Extract chat room ID from destination like "/topic/chat/123"
                	Long chatRoomId = extractChatRoomIdFromDestination(destination);
                    
                    if (chatRoomId != null) {
                        // Check if user is participant in this chat room
                        Long userId = getUserIdFromUsername(username);
                        if (userId != null && !participantService.isUserInChatRoom(userId, chatRoomId)) {
                            log.warn("User {} denied subscription to chat room {}", 
                            		 username, chatRoomId);
                            
                            // Send error message to user
                            messagingTemplate.convertAndSendToUser(
                                username, 
                                "/queue/errors", 
                                "Access denied to chat room " + chatRoomId
                            );
                        } else {
                            log.debug("User {} granted access to chat room {} topic: {}", 
                                     username, chatRoomId, destination);
                        }
                    }
                } else if (destination.startsWith("/user/queue/")) {
                    // User-specific queues (/user/queue/chat-updates, /user/queue/errors, etc.)
                    log.debug("User {} subscribed to personal queue: {}", 
                             username, destination);
                }
                
            } catch (Exception e) {
                log.error("Error validating subscription: {}", e.getMessage());
            }
        }
    }
    
    private void logActiveUsers(String event) {
        Set<SimpUser> users = userRegistry.getUsers();
        log.info("{} - Active users: {}", event, users.size());
                
        for (SimpUser user : users) {
            log.info("Active user: {} with {} sessions", 
                    user.getName(), user.getSessions().size());
        }
    }
    
    /**
     * Extract chat room ID from various destination patterns:
     * - /topic/chat/123 -> 123
     * - /topic/chat/123/updates -> 123
     * - /topic/chat/123/status -> 123
     */
    private Long extractChatRoomIdFromDestination(String destination) {
        try {
            if (!destination.startsWith("/topic/chat/")) {
                return null;
            }
            
            // Remove the "/topic/chat/" prefix
            String remaining = destination.substring("/topic/chat/".length());
            
            // Find the first slash or use the entire remaining string
            int slashIndex = remaining.indexOf('/');
            String chatRoomIdStr = slashIndex != -1 ? remaining.substring(0, slashIndex) : remaining;
            
            // Parse the chat room ID
            return Long.parseLong(chatRoomIdStr);
            
        } catch (NumberFormatException e) {
            log.error("Failed to extract chat room ID from destination: {}", 
                     destination);
            return null;
        }
    }
    
    private Long getUserIdFromUsername(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isPresent()) {
            Long userId = userOpt.get().getId();
            log.debug("Found user ID {} for username {}", userId, username);
            return userId;
        } else {
            log.warn("No user found with username {}", 
            		 username);
            return null;
        }
    }
}
