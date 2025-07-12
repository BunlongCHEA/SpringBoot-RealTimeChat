package com.project.realtimechat.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.realtimechat.config.WebSocketEventListener;
import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.ChatMessageDTO;
import com.project.realtimechat.entity.EnumMessageType;
import com.project.realtimechat.entity.User;
import com.project.realtimechat.repository.UserRepository;
import com.project.realtimechat.service.ChatMessageService;
import com.project.realtimechat.service.ParticipantService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/messages")
public class ChatMessageController {
	private static final Logger log = LoggerFactory.getLogger(ChatMessageController.class);
	
	private static final String utcString = Instant.now().toString();
	
	@Autowired
	private ChatMessageService chatMessageService;
	
	@Autowired
	private ParticipantService participantService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	
    
	/**
     * WebSocket endpoint for sending text messages to a chat room
     * The message will be broadcast to all subscribers of the chat room topic
     * @param chatRoomId The ID of the chat room
     * @param messagePayload The message payload containing content
     * @param authentication The authenticated user
     */
    @MessageMapping("/chat.sendMessage/{chatRoomId}")
    public void sendMessage(
    		@DestinationVariable Long chatRoomId, 
    		@Payload Map<String, Object> messagePayload,
    		Authentication authentication
    		) {
    	if (authentication == null) {
            log.error("Unauthenticated user attempted to send message to room {}", chatRoomId);
            return;
        }
        
        String username = authentication.getName();
        log.info("[{}] | User {} is sending message to room {}", 
        		Instant.now(), username, chatRoomId);
        
        try {
            // Get user ID from authenticated username
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + username));
            
            // Check if user is a participant in this chat room
            if (!participantService.isUserInChatRoom(user.getId(), chatRoomId)) {
                log.warn("User {} is not a participant in chat room {}", username, chatRoomId);
                // Send error message to user
                messagingTemplate.convertAndSendToUser(
                    username, 
                    "/queue/errors", 
                    "You are not a participant in this chat room"
                );
                return;
            }
            
            String content = (String) messagePayload.get("content");
            if (content == null || content.trim().isEmpty()) {
                log.warn("Empty message content from user {}", username);
                return;
            }
            
            // Create message through service
            ResponseEntity<BaseDTO<ChatMessageDTO>> response = 
                    chatMessageService.createTextMessage(chatRoomId, user.getId(), content);
            
            if (response.getBody() != null && response.getBody().getData() != null) {
                ChatMessageDTO messageDTO = response.getBody().getData();
                
                // Add sender username to the message
                messageDTO.setSenderName(username);
                
                // Broadcast to all subscribers of this chat room
                messagingTemplate.convertAndSend(
                        "/topic/chat/" + chatRoomId, 
                        messageDTO);
                
                log.info("[{}] | Message from {} sent to room {} successfully", 
                		Instant.now(), username, chatRoomId);
            }
            
        } catch (Exception e) {
            log.error("[{}] | Error sending message from {} to room {}: {}", 
            		Instant.now(), username, chatRoomId, e.getMessage());
            
            // Send error message to user
            messagingTemplate.convertAndSendToUser(
                username, 
                "/queue/errors", 
                "Failed to send message: " + e.getMessage()
            );
        }
    }
    
    /**
     * WebSocket endpoint for typing indicators
     * @param chatRoomId The ID of the chat room
     * @param typingPayload The typing payload
     * @param authentication The authenticated user
     */
    @MessageMapping("/chat.typing/{chatRoomId}")
    public void sendTypingIndicator(
    		@DestinationVariable Long chatRoomId,
    		@Payload Map<String, Object> typingPayload,
    		Authentication authentication
    		) {
    	if (authentication == null) {
            return;
        }
        
        String username = authentication.getName();
        Boolean isTyping = (Boolean) typingPayload.get("isTyping");
        
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + username));
            
            // Check if user is a participant in this chat room
            if (!participantService.isUserInChatRoom(user.getId(), chatRoomId)) {
                return;
            }
            
            // Create typing indicator message
            Map<String, Object> typingMessage = new HashMap<>();
            typingMessage.put("type", "typing");
            typingMessage.put("userId", user.getId());
            typingMessage.put("username", username);
            typingMessage.put("isTyping", isTyping != null ? isTyping : false);
            typingMessage.put("timestamp", Instant.now().toString());
            
            // Broadcast typing indicator to all other participants
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatRoomId + "/typing", 
                    typingMessage);
            
        } catch (Exception e) {
            log.error("Error handling typing indicator from {} in room {}: {}", 
                    username, chatRoomId, e.getMessage());
        }
    }
    
    /**
     * WebSocket endpoint for joining a chat room
     * @param chatRoomId The ID of the chat room
     * @param authentication The authenticated user
     */
    @MessageMapping("/chat.join/{chatRoomId}")
    public void joinChatRoom(
    		@DestinationVariable Long chatRoomId,
    		Authentication authentication
    		) {
    	if (authentication == null) {
            return;
        }
        
        String username = authentication.getName();
        log.info("[{}] | User {} is joining chat room {}", 
        		Instant.now(), username, chatRoomId);
        
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + username));
            
            // Check if user is a participant in this chat room
            if (!participantService.isUserInChatRoom(user.getId(), chatRoomId)) {
                log.warn("User {} is not authorized to join chat room {}", username, chatRoomId);
                return;
            }
            
            // Create join notification
            Map<String, Object> joinMessage = new HashMap<>();
            joinMessage.put("type", "user_joined");
            joinMessage.put("userId", user.getId());
            joinMessage.put("username", username);
            joinMessage.put("timestamp", Instant.now().toString());
            
            // Broadcast join notification to room
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatRoomId + "/events", 
                    joinMessage);
            
            log.info("[{}] | User {} joined chat room {} successfully", 
            		Instant.now(), username, chatRoomId);
            
        } catch (Exception e) {
            log.error("Error handling join for user {} in room {}: {}", 
                    username, chatRoomId, e.getMessage());
        }
    }
    
    /**
     * WebSocket endpoint for leaving a chat room
     * @param chatRoomId The ID of the chat room
     * @param authentication The authenticated user
     */
    @MessageMapping("/chat.leave/{chatRoomId}")
    public void leaveChatRoom(
    		@DestinationVariable Long chatRoomId,
    		Authentication authentication
    		) {
    	if (authentication == null) {
            return;
        }
        
        String username = authentication.getName();
        log.info("[{}] | User {} is leaving chat room {}", 
        		Instant.now(), username, chatRoomId);
        
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + username));
            
            // Create leave notification
            Map<String, Object> leaveMessage = new HashMap<>();
            leaveMessage.put("type", "user_left");
            leaveMessage.put("userId", user.getId());
            leaveMessage.put("username", username);
            leaveMessage.put("timestamp", Instant.now().toString());
            
            // Broadcast leave notification to room
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatRoomId + "/events", 
                    leaveMessage);
            
            log.info("[{}] | User {} left chat room {} successfully", 
            		Instant.now(), username, chatRoomId);
            
        } catch (Exception e) {
            log.error("Error handling leave for user {} in room {}: {}", 
                    username, chatRoomId, e.getMessage());
        }
    }
    
    // REST API Methods (only essential ones)
    
    /**
     * Retrieves messages for a chat room with pagination
     * @param chatRoomId The ID of the chat room
     * @param pageable Pagination parameters
     * @param authentication The authenticated user
     */
    @GetMapping("/chat-room/{chatRoomId}")
    public ResponseEntity<BaseDTO<List<ChatMessageDTO>>> getMessagesByChatRoomId(
            @PathVariable Long chatRoomId,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + username));
            
            // Check if user is a participant in this chat room
            if (!participantService.isUserInChatRoom(user.getId(), chatRoomId)) {
                throw new IllegalStateException("User is not a participant in this chat room");
            }
            
            return chatMessageService.getMessagesByChatRoomId(chatRoomId, pageable);
        } catch (Exception e) {
            log.error("Error retrieving messages: {}", e.getMessage());
            throw e;
        }
    }
    
    
    
}
