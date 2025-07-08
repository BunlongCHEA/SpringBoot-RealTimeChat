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
	
    
    // METHOD : Start Communication method for real time chat
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
    	String username = authentication.getName();
        log.info("[{}] | User {} is sending message to room {}", 
        		utcString, username, chatRoomId);
        
        try {
            // Get user ID from authenticated username
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + username));
            
            // Check if user is a participant in this chat room
            if (!participantService.isUserInChatRoom(user.getId(), chatRoomId)) {
                throw new IllegalStateException("User is not a participant in this chat room");
            }
            
            String content = (String) messagePayload.get("content");
            
            // Create message through service
            ResponseEntity<BaseDTO<ChatMessageDTO>> response = 
                    chatMessageService.createTextMessage(chatRoomId, user.getId(), content);
            
            ChatMessageDTO messageDTO = response.getBody().getData();
            
            // Add sender username to the message
            messageDTO.setSenderName(username);
            
            // Broadcast to all subscribers of this chat room
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatRoomId, 
                    messageDTO);
            
            log.info("[{}] | Message from {} sent to room {}", 
            		utcString, username, chatRoomId);
        } catch (Exception e) {
            log.error("[{}] | Error sending message to room {}: {}", 
            		utcString, chatRoomId, e.getMessage());
            
            // Send error notification back to sender
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to send message: " + e.getMessage());
            
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/errors",
                    errorResponse);
        }
    }
    
    /**
     * WebSocket endpoint for sending messages with attachments to a chat room
     * @param chatRoomId The ID of the chat room
     * @param messagePayload The message payload containing content and attachment URLs
     * @param authentication The authenticated user
     */
    @MessageMapping("/chat.sendAttachment/{chatRoomId}")
    public void sendAttachment(
            @DestinationVariable Long chatRoomId,
            @Payload Map<String, Object> messagePayload,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("[{}] | User {} is sending attachment to room {}", 
        		utcString, username, chatRoomId);
        
        try {
            // Get user ID from authenticated username
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + username));
            
            String content = (String) messagePayload.getOrDefault("content", "");
            String messageType = (String) messagePayload.get("type");
            
            EnumMessageType type;
            if ("IMAGE".equalsIgnoreCase(messageType)) {
                type = EnumMessageType.IMAGE;
            } else if ("FILE".equalsIgnoreCase(messageType)) {
                type = EnumMessageType.FILE;
            } else {
                throw new IllegalArgumentException("Invalid attachment type: " + messageType);
            }
            
            // Extract attachment URLs
            @SuppressWarnings("unchecked")
            Set<String> attachmentUrls = new HashSet<>((Set<String>) messagePayload.get("attachmentUrls"));
            
            if (attachmentUrls == null || attachmentUrls.isEmpty()) {
                throw new IllegalArgumentException("Attachment URLs are required");
            }
            
            // Create message through service
            ResponseEntity<BaseDTO<ChatMessageDTO>> response = 
                    chatMessageService.createMessageWithAttachments(
                            chatRoomId, user.getId(), content, type, attachmentUrls);
            
            ChatMessageDTO messageDTO = response.getBody().getData();
            
            // Add sender username to the message
            messageDTO.setSenderName(username);
            
            // Broadcast to all subscribers of this chat room
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatRoomId, 
                    messageDTO);
            
            log.info("[{}] | {} attachment from {} sent to room {}}", 
            		utcString, type, username, chatRoomId);
        } catch (Exception e) {
            log.error("[{}] | Error sending attachment to room {}: {}", 
            		utcString, chatRoomId, e.getMessage());
            
            // Send error notification back to sender
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to send attachment: " + e.getMessage());
            
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/errors",
                    errorResponse);
        }
    }
    
    /**
     * WebSocket endpoint for sending system messages to a chat room
     * Only users with ADMIN role can send system messages
     * @param chatRoomId The ID of the chat room
     * @param messagePayload The message payload containing system message content
     * @param authentication The authenticated user
     */
    @MessageMapping("/chat.sendSystem/{chatRoomId}")
    public void sendSystemMessage(
            @DestinationVariable Long chatRoomId,
            @Payload Map<String, String> messagePayload,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("[{}] | User {} is sending system message to room {}", 
        		utcString, username, chatRoomId);
        
        try {
            // Get user ID from authenticated username
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + username));
            
            // Check if user is an admin in this chat room
            boolean isAdmin = participantService.isUserAdminInChatRoom(chatRoomId, user.getId());
            
            if (!isAdmin) {
                throw new IllegalStateException("Only administrators can send system messages");
            }
            
            String content = messagePayload.get("content");
            
            // Create system message through service
            ResponseEntity<BaseDTO<ChatMessageDTO>> response = 
                    chatMessageService.createSystemMessage(chatRoomId, content);
            
            ChatMessageDTO messageDTO = response.getBody().getData();
            
            // Broadcast to all subscribers of this chat room
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatRoomId, 
                    messageDTO);
            
            log.info("[{}] | System message sent to room {}", 
            		utcString, chatRoomId);
        } catch (Exception e) {
            log.error("[{}] | Error sending system message to room {}: {}", 
            		utcString, chatRoomId, e.getMessage());
            
            // Send error notification back to sender
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to send system message: " + e.getMessage());
            
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/errors",
                    errorResponse);
        }
    }
    
    /**
     * WebSocket endpoint for updating message status (read/delivered)
     * @param messageId The ID of the message
     * @param statusPayload The status update payload
     * @param authentication The authenticated user
     */
    @MessageMapping("/chat.updateStatus/{messageId}")
    public void updateMessageStatus(
            @DestinationVariable Long messageId,
            @Payload Map<String, Boolean> statusPayload,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("[{}] | User {} is updating message status for message {}", 
                utcString, username, messageId);
        
        try {
            // Get user ID from authenticated username
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + username));
            
            Boolean read = statusPayload.getOrDefault("read", false);
            Boolean delivered = statusPayload.getOrDefault("delivered", true); // Default to delivered
            
            // Update message status through service
            ResponseEntity<BaseDTO<ChatMessageDTO>> response = 
                    chatMessageService.updateMessageStatus(messageId, user.getId(), read, delivered);
            
            ChatMessageDTO messageDTO = response.getBody().getData();
            
            // Get the chat room ID from the message
            Long chatRoomId = messageDTO.getChatRoomId();
            
            // Broadcast status update to the chat room
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("messageId", messageId);
            statusUpdate.put("userId", user.getId());
            statusUpdate.put("username", username);
            statusUpdate.put("status", read ? "READ" : (delivered ? "DELIVERED" : "SENT"));
            statusUpdate.put("timestamp", Instant.now().toString());
            
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatRoomId + "/status", 
                    statusUpdate);
            
            log.info("[{}] | Message {} status updated to {} by {}", 
                    utcString, messageId, (read ? "READ" : "DELIVERED"), username);
        } catch (Exception e) {
            log.error("[{}] | Error updating message status: {}", 
                    utcString, e.getMessage());
            
            // Send error notification back to sender
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to update message status: " + e.getMessage());
            
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/errors",
                    errorResponse);
        }
    }
    
    /**
     * WebSocket endpoint for editing a message
     * @param messageId The ID of the message to edit
     * @param messagePayload The updated message content
     * @param authentication The authenticated user
     */
    @MessageMapping("/chat.editMessage/{messageId}")
    public void editMessage(
            @DestinationVariable Long messageId,
            @Payload Map<String, String> messagePayload,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("[{}] | User {} is editing message {}", 
                utcString, username, messageId);
        
        try {
            // Get user ID from authenticated username
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + username));
            
            String newContent = messagePayload.get("content");
            
            // Check permission
            if (!chatMessageService.isUserAllowedToModifyMessage(messageId, user.getId())) {
                throw new IllegalStateException("You don't have permission to edit this message");
            }
            
            // Edit message through service
            ResponseEntity<BaseDTO<ChatMessageDTO>> response = 
                    chatMessageService.editChatMessage(messageId, user.getId(), newContent);
            
            ChatMessageDTO messageDTO = response.getBody().getData();
            
            // Add edited flag
            Map<String, Object> editedMessage = new HashMap<>();
            editedMessage.put("messageId", messageId);
            editedMessage.put("content", newContent);
            editedMessage.put("edited", true);
            editedMessage.put("editedAt", Instant.now().toString());
            
            // Broadcast to all subscribers of this chat room
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + messageDTO.getChatRoomId() + "/edit", 
                    editedMessage);
            
            log.info("[{}] | Message {} edited by {}", 
                    utcString, messageId, username);
        } catch (Exception e) {
            log.error("[{}] | Error editing message {}: {}", 
                    utcString, messageId, e.getMessage());
            
            // Send error notification back to sender
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to edit message: " + e.getMessage());
            
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/errors",
                    errorResponse);
        }
    }
    
    /**
     * WebSocket endpoint for deleting a message
     * @param messageId The ID of the message to delete
     * @param authentication The authenticated user
     */
    @MessageMapping("/chat.deleteMessage/{messageId}")
    public void deleteMessage(
            @DestinationVariable Long messageId,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("[{}] | User {} is deleting message {} at {}", 
                utcString, username, messageId);
        
        try {
            // Get user ID from authenticated username
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + username));
            
            // Get chat room ID before deleting the message
            Long chatRoomId = chatMessageService.findEntityByChatMessageId(messageId).getChatRooms().getId();
            
            // Check permission
            if (!chatMessageService.isUserAllowedToModifyMessage(messageId, user.getId())) {
                throw new IllegalStateException("You don't have permission to delete this message");
            }
            
            // Delete message through service
            chatMessageService.deleteChatMessage(messageId, user.getId());
            
            // Send deletion notification
            Map<String, Object> deletionNotice = new HashMap<>();
            deletionNotice.put("messageId", messageId);
            deletionNotice.put("deleted", true);
            deletionNotice.put("deletedBy", username);
            deletionNotice.put("deletedAt", Instant.now().toString());
            
            // Broadcast to all subscribers of this chat room
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatRoomId + "/delete", 
                    deletionNotice);
            
            log.info("[{}] | Message {} deleted by {}", 
            		utcString, messageId, username);
        } catch (Exception e) {
            log.error("[{}] | Error deleting message {}: {}", 
                    utcString, messageId, e.getMessage());
            
            // Send error notification back to sender
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to delete message: " + e.getMessage());
            
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/errors",
                    errorResponse);
        }
    }
    
    /**
     * REST endpoint to get a user's unread message count in a chat room
     * @param chatRoomId The ID of the chat room
     * @param authentication The authenticated user
     * @return The unread message count
     */
    @GetMapping("/unread/{chatRoomId}")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @PathVariable Long chatRoomId,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("[{}] | User {} is checking unread count for room {}", 
                utcString, username, chatRoomId);
        
        try {
            // Get user ID from authenticated username
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + username));
            
            // Get count of unread messages since user's last activity
            Instant since = Instant.now().minusSeconds(86400); // Default to last 24 hours
            
            // You might want to get this from participant's last seen time instead
            Long unreadCount = chatMessageService.getUnreadMessageCount(chatRoomId, user.getId(), since);
            
            Map<String, Object> response = new HashMap<>();
            response.put("chatRoomId", chatRoomId);
            response.put("unreadCount", unreadCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[{}] | Error getting unread count for room {}: {}", 
                    utcString, chatRoomId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to get unread count: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
	
    // METHOD: Other method for helper
	@GetMapping("/{id}")
	public ResponseEntity<BaseDTO<ChatMessageDTO>> getChatMessageById(@PathVariable Long id) {
		return chatMessageService.getChatMessageById(id);
	}
	
	@GetMapping("/room/{chatRoomId}")
	public ResponseEntity<BaseDTO<List<ChatMessageDTO>>> getChatMessagesByChatRoomId(@PathVariable Long chatRoomId) {
        return chatMessageService.getChatMessagesByChatRoomId(chatRoomId);
    }
	
	@GetMapping("/room/{chatRoomId}/paginated")
	public ResponseEntity<BaseDTO<Page<ChatMessageDTO>>> getChatMessagesPaginated(
			@PathVariable Long chatRoomId,
			@PageableDefault(size = 20) Pageable pageable) {
        return chatMessageService.getChatMessagesPaginated(chatRoomId, pageable);
    }
	
	@GetMapping("/room/{chatRoomId}/after")
	public ResponseEntity<BaseDTO<List<ChatMessageDTO>>> getChatMessagesAfterTimestamp(
            @PathVariable Long chatRoomId,
            @RequestParam Long timestamp) {
        return chatMessageService.getChatMessagesAfterTimestamp(chatRoomId, Instant.ofEpochMilli(timestamp));
    }
	
	@PostMapping("/room/{chatRoomId}/text")
	public ResponseEntity<BaseDTO<ChatMessageDTO>> createTextMessage(
            @PathVariable Long chatRoomId,
            @RequestParam Long senderId,
            @Valid @RequestBody String content) {
        return chatMessageService.createTextMessage(
                chatRoomId, senderId, content);
    }
	
	@PostMapping("/room/{chatRoomId}/system")
	public ResponseEntity<BaseDTO<ChatMessageDTO>> createSystemMessage(
            @PathVariable Long chatRoomId,
            @RequestBody String content) {
        return chatMessageService.createSystemMessage(chatRoomId, content);
    }
	
	@PostMapping("/room/{chatRoomId}/attachment")
	public ResponseEntity<BaseDTO<ChatMessageDTO>> createMessageWithAttachments(
            @RequestParam Long chatRoomId,
            @RequestParam Long senderId,
            @RequestBody(required = false) String content,
            @RequestBody EnumMessageType type,
            @RequestBody Set<String> attachmentUrls
    ) {
        return chatMessageService.createMessageWithAttachments(
                chatRoomId, senderId, content, type, attachmentUrls);
    }
	
	@DeleteMapping("/{messageId}")
	public ResponseEntity<BaseDTO<Void>> deleteChatMessage(
            @PathVariable Long messageId,
            @RequestParam Long userId) {
        return chatMessageService.deleteChatMessage(messageId, userId);
    }
	
	@PutMapping("/{messageId}")
	public ResponseEntity<BaseDTO<ChatMessageDTO>> editChatMessage(
            @PathVariable Long messageId,
            @RequestParam Long userId,
            @RequestParam String newContent) {
        return chatMessageService.editChatMessage(messageId, userId, newContent);
    }
	
	@GetMapping("/room/{chatRoomId}/search")
	public ResponseEntity<BaseDTO<List<ChatMessageDTO>>> searchMessagesByContent(
            @PathVariable Long chatRoomId,
            @RequestParam String searchTerm) {
        return chatMessageService.searchMessagesByContent(chatRoomId, searchTerm);
    }
	
	@GetMapping("/room/{chatRoomId}/user/{userId}")
	public ResponseEntity<BaseDTO<List<ChatMessageDTO>>> getMessagesByUser(
            @PathVariable Long chatRoomId,
            @PathVariable Long userId) {
        return chatMessageService.getMessagesByUser(chatRoomId, userId);
    }
	
	@GetMapping("/room/{chatRoomId}/type/{type}")
	public ResponseEntity<BaseDTO<List<ChatMessageDTO>>> getMessagesByType(
            @PathVariable Long chatRoomId,
            @PathVariable EnumMessageType type) {
        return chatMessageService.getMessagesByType(chatRoomId, type);
    }
	
	@PutMapping("/{messageId}/status")
	public ResponseEntity<BaseDTO<ChatMessageDTO>> updateMessageStatus(
            @PathVariable Long messageId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean read,
            @RequestParam(defaultValue = "false") boolean delivered) {
        return chatMessageService.updateMessageStatus(messageId, userId, read, delivered);
    }
}
