package com.project.realtimechat.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Base64;
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
import org.springframework.web.multipart.MultipartFile;

import com.project.realtimechat.config.WebSocketEventListener;
import com.project.realtimechat.config.WebSocketEventPublisher;
import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.ChatMessageDTO;
import com.project.realtimechat.entity.EnumMessageType;
import com.project.realtimechat.entity.User;
import com.project.realtimechat.exception.BadRequestException;
import com.project.realtimechat.repository.UserRepository;
import com.project.realtimechat.service.ChatMessageService;
import com.project.realtimechat.service.ParticipantService;

// import io.jsonwebtoken.io.IOException;
import java.io.IOException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/messages")
public class ChatMessageController {
	private static final Logger log = LoggerFactory.getLogger(ChatMessageController.class);
	
//	private static final String utcString = Instant.now().toString();
	
	@Autowired
	private ChatMessageService chatMessageService;
	
	@Autowired
	private ParticipantService participantService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	
	@Autowired
    private WebSocketEventPublisher eventPublisher;
    
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
        log.info("User {} is sending message to room {}", username, chatRoomId);
        
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
                
                // Broadcast to all participants for sidebar updates
                eventPublisher.broadcastMessageSentNotification(chatRoomId, messageDTO);
                
                log.info("Message from {} sent to room {} successfully", username, chatRoomId);
            }
            
        } catch (Exception e) {
            log.error("Error sending message from {} to room {}: {}", username, chatRoomId, e.getMessage());
            
            // Send error message to user
            messagingTemplate.convertAndSendToUser(
                username, 
                "/queue/errors", 
                "Failed to send message: " + e.getMessage()
            );
        }
    }
    
    /**
     * WebSocket endpoint for sending image messages to a chat room
     * Supports both image upload (base64) and image URL
     * JSON payload format:
     * Option 1 - Upload image: { "imageData": "base64String", "filename": "image.jpg", "contentType": "image/jpeg" }
     * Option 2 - Image URL: { "imageUrl": "https://example.com/image.jpg" }
     * @param chatRoomId The ID of the chat room
     * @param messagePayload The message payload containing image data or URL
     * @param authentication The authenticated user
     */
    @MessageMapping("/chat.sendImage/{chatRoomId}")
    public void sendImageMessage(
            @DestinationVariable Long chatRoomId,
            @Payload Map<String, Object> messagePayload,
            Authentication authentication) {
                
        log.info("CONTROLLER METHOD INVOKED: sendImageMessage");
    	
    	if (authentication == null) {
            log.error("Unauthenticated user attempted to send image to room {}", chatRoomId);
            messagingTemplate.convertAndSendToUser(
                "anonymous", 
                "/queue/errors", 
                "Authentication required"
            );
            return;
        }
    	
    	String username = authentication.getName();
        log.info("User {} is sending image to room {}", 
                username, chatRoomId);
        
        try {
        	// Get user ID from authenticated username
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + username));
            
            // Check if user is a participant in this chat room
            if (!participantService.isUserInChatRoom(user.getId(), chatRoomId)) {
                log.warn("User {} is not a participant in chat room {}", username, chatRoomId);
                messagingTemplate.convertAndSendToUser(
                    username, 
                    "/queue/errors", 
                    "You are not a participant in this chat room"
                );
                return;
            }
            
            ResponseEntity<BaseDTO<ChatMessageDTO>> response;
            
            // Option 1: Upload & Send image (createImageMessage) - Base64 encoded image
            String imageData = (String) messagePayload.get("imageData");
            String filename = (String) messagePayload.get("filename");
            String contentType = (String) messagePayload.get("contentType");
            
            if (imageData != null && !imageData.trim().isEmpty()) {
                log.info("Processing base64 image upload for user {} in room {}", 
                        username, chatRoomId);
                
                try {
                    // Convert base64 to MultipartFile
                    MultipartFile imageFile = convertBase64ToMultipartFile(imageData, filename, contentType);
                    response = chatMessageService.createImageMessage(chatRoomId, user.getId(), imageFile);
                    
                    log.info("Base64 image uploaded and message created for user {} in room {}", 
                            username, chatRoomId);
                } catch (Exception e) {
                    log.error("Error processing base64 image for user {} in room {}: {}", 
                            username, chatRoomId, e.getMessage());
                    messagingTemplate.convertAndSendToUser(
                        username, 
                        "/queue/errors", 
                        "Failed to process image upload: " + e.getMessage()
                    );
                    return;
                }
            }
            // Option 2: Send image from URL (createImageMessageFromUrl)
            else {
                String imageUrl = (String) messagePayload.get("imageUrl");
                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    log.warn("Neither image data nor image URL provided by user {}", username);
                    messagingTemplate.convertAndSendToUser(
                        username, 
                        "/queue/errors", 
                        "Either imageData or imageUrl must be provided"
                    );
                    return;
                }
                
                log.info("Processing image URL for user {} in room {}: {}", 
                        username, chatRoomId, imageUrl);
                
                response = chatMessageService.createImageMessageFromUrl(chatRoomId, user.getId(), imageUrl);
                log.info("Image message from URL created for user {} in room {}", 
                        username, chatRoomId);
            }
            
            // Broadcast message if successful
            if (response.getBody() != null && response.getBody().getData() != null) {
                ChatMessageDTO messageDTO = response.getBody().getData();
                
                // Add sender username to the message
                messageDTO.setSenderName(username);
                
                // Broadcast to all subscribers of this chat room
                messagingTemplate.convertAndSend(
                        "/topic/chat/" + chatRoomId, 
                        messageDTO);
                
                log.info("Image message from {} sent to room {} successfully", 
                        username, chatRoomId);

                // Broadcast to global message notifications (for ChatSidebar)
                eventPublisher.broadcastMessageSentNotification(chatRoomId, messageDTO);
                
                log.info("Image message from {} sent to room {} successfully - MessageId: {}, Content: \"{}\"", 
                        username, chatRoomId, messageDTO.getId(), messageDTO.getContent());
            }
            
        } catch (Exception e) {
            log.error("Error sending image message from {} to room {}: {}", 
                    username, chatRoomId, e.getMessage());
            
            messagingTemplate.convertAndSendToUser(
                username, 
                "/queue/errors", 
                "Failed to send image message: " + e.getMessage()
            );
        }
    }
    
    /**
     * Helper method to convert base64 string to MultipartFile
     * @param base64Data The base64 encoded image data
     * @param filename The original filename
     * @param contentType The content type of the image
     * @return MultipartFile object
     */
    private MultipartFile convertBase64ToMultipartFile(String base64Data, String filename, String contentType) {
        try {
            // Remove data URL prefix if present (e.g., "data:image/jpeg;base64,")
            if (base64Data.contains(",")) {
                base64Data = base64Data.split(",")[1];
            }
            
            byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
            
            return new MultipartFile() {
                @Override
                public String getName() {
                    return "image";
                }
                
                @Override
                public String getOriginalFilename() {
                    return filename != null ? filename : "image.jpg";
                }
                
                @Override
                public String getContentType() {
                    return contentType != null ? contentType : "image/jpeg";
                }
                
                @Override
                public boolean isEmpty() {
                    return decodedBytes.length == 0;
                }
                
                @Override
                public long getSize() {
                    return decodedBytes.length;
                }
                
                @Override
                public byte[] getBytes() throws IOException {
                    return decodedBytes;
                }
                
                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(decodedBytes);
                }
                
                @Override
                public void transferTo(File dest) throws IOException, IllegalStateException {
                    try (FileOutputStream fos = new FileOutputStream(dest)) {
                        fos.write(decodedBytes);
                    } catch (FileNotFoundException e) {
                        log.error("File not found: {}", e.getMessage());
                        throw new IOException("Could not create file: " + dest.getAbsolutePath(), e);
                    } catch (IOException e) {
                        log.error("Error writing to file: {}", e.getMessage());
                        throw new IOException("Error writing to file: " + dest.getAbsolutePath(), e);
                    }
                }
            };
        } catch (IllegalArgumentException e) {
            log.error("Invalid base64 data: {}", e.getMessage());
            throw new BadRequestException("Invalid base64 image data: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error converting base64 to multipart file: {}", e.getMessage());
            throw new BadRequestException("Failed to process image: " + e.getMessage());
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
        log.info("User {} is joining chat room {}", 
        		username, chatRoomId);
        
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
            
            log.info("User {} joined chat room {} successfully", 
            		username, chatRoomId);
            
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
        log.info("User {} is leaving chat room {}", 
        		username, chatRoomId);
        
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
            
            log.info("User {} left chat room {} successfully", 
            		username, chatRoomId);
            
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
    @GetMapping("/room/{chatRoomId}")
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
            log.error("Error retrieving messages: {}",e.getMessage());
            throw e;
        }
    }
    
    @GetMapping("/{messageId}")
    public ResponseEntity<BaseDTO<ChatMessageDTO>> getMessageById(@PathVariable Long messageId) {
        return chatMessageService.getMessageById(messageId);
    }
}