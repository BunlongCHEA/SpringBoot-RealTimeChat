package com.project.realtimechat.config;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.project.realtimechat.dto.ParticipantDTO;
import com.project.realtimechat.entity.User;
import com.project.realtimechat.repository.UserRepository;
import com.project.realtimechat.service.ParticipantService;

@Component
public class WebSocketEventListener {
//	private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
//	
//	private static final String utcString = Instant.now().toString();
//
//    @Autowired
//    private SimpMessageSendingOperations messagingTemplate;
//    
//    @Autowired
//    private ParticipantService participantService;
//    
//    @Autowired
//    private UserRepository userRepository;
//
//    @EventListener
//    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
//        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
//        Authentication auth = (Authentication) headerAccessor.getUser();
//        
//        if (auth != null) {
//            String username = auth.getName();
//            log.info("[{}] | User {} connected to WebSocket", 
//                    utcString, username);
//            
//            // Update user online status
//            try {
//                Long userId = getUserIdFromUsername(username);
//                if (userId != null) {
//                	// Update online status using the new service method
//                    participantService.updateOnlineStatus(userId, true);
//                    
//                    // Broadcast online status to users in same chat rooms
//                    broadcastUserStatusUpdate(userId, username, true);
//                }
//            } catch (Exception e) {
//                log.error("[{}] | Error updating user online status: {}", utcString, e.getMessage());
//            }
//        }
//    }
//
//    @EventListener
//    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
//        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
//        Authentication auth = (Authentication) headerAccessor.getUser();
//        
//        if (auth != null) {
//            String username = auth.getName();
//            log.info("[{}] | User {} disconnected from WebSocket", 
//                    utcString, username);
//            
//            // Update user offline status
//            try {
//                Long userId = getUserIdFromUsername(username);
//                if (userId != null) {
//                	// Update offline status using the new service method
//                    participantService.updateOnlineStatus(userId, false);
//                    
//                    // Broadcast offline status to users in same chat rooms
//                    broadcastUserStatusUpdate(userId, username, false);
//                }
//            } catch (Exception e) {
//                log.error("[{}] | Error updating user offline status: {}", utcString, e.getMessage());
//            }
//        }
//    }
//    
//    /**
//     * Handles when users subscribe to channels/topics
//     * This validates if they have permission to subscribe
//     */
//    @EventListener
//    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
//        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
//        Authentication auth = (Authentication) headerAccessor.getUser();
//        String destination = headerAccessor.getDestination();
//        
//        if (auth != null && destination != null) {
//            String username = auth.getName();
//            log.info("[{}] | User {} subscribed to {}", 
//                    utcString, username, destination);
//            
//            // Validate subscription permission
//            try {
//                if (destination.startsWith("/topic/chat/")) {
//                    // Extract chat room ID from destination like "/topic/chat/123"
//                    String chatRoomIdStr = destination.substring("/topic/chat/".length());
//                    Long chatRoomId = Long.parseLong(chatRoomIdStr);
//                    
//                    // Check if user is participant in this chat room
//                    Long userId = getUserIdFromUsername(username);
//                    if (userId != null && !participantService.isUserInChatRoom(userId, chatRoomId)) {
//                        log.warn("[{}] | User {} denied subscription to chat room {}", 
//                                utcString, username, chatRoomId);
//                        
//                        // Send error message to user
//                        messagingTemplate.convertAndSendToUser(
//                            username, 
//                            "/queue/errors", 
//                            "Access denied to chat room " + chatRoomId
//                        );
//                    }
//                }
//            } catch (Exception e) {
//                log.error("[{}] | Error validating subscription: {}", utcString, e.getMessage());
//            }
//        }
//    }
//    
//    /**
//     * Broadcasts user status updates to other users in the same chat rooms
//     */
//    private void broadcastUserStatusUpdate(Long userId, String username, boolean online) {
//        try {
//            // Get all chat rooms where this user is a participant
//            List<ParticipantDTO> userParticipations = participantService
//                    .getParticipantsByUserId(userId)
//                    .getBody()
//                    .getData();
//            
//            // Create status update message
//            Map<String, Object> statusUpdate = new HashMap<>();
//            statusUpdate.put("userId", userId);
//            statusUpdate.put("username", username);
//            statusUpdate.put("online", online);
//            statusUpdate.put("lastSeen", Instant.now().toString());
//            
//            // Broadcast to each chat room
//            for (ParticipantDTO participation : userParticipations) {
//                String destination = "/topic/chat/" + participation.getChatRoomId() + "/status";
//                messagingTemplate.convertAndSend(destination, statusUpdate);
//                
//                log.debug("[{}] | Broadcasted status update for user {} to chat room {}", 
//                        utcString, username, participation.getChatRoomId());
//            }
//        } catch (Exception e) {
//            log.error("[{}] | Failed to broadcast status update for user {}: {}", 
//                    utcString, username, e.getMessage());
//        }
//    }
//    
//    private Long getUserIdFromUsername(String username) {
//        Optional<User> userOpt = userRepository.findByUsername(username);
//        
//        if (userOpt.isPresent()) {
//            Long userId = userOpt.get().getId();
//            log.debug("[{}] | Found user ID {} for username {}", 
//                    utcString, userId, username);
//            return userId;
//        } else {
//            log.warn("[{}] | No user found with username {}", 
//                    utcString, username);
//            return null;
//        }
//    }
}
