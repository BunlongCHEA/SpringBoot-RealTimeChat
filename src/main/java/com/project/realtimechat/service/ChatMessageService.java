package com.project.realtimechat.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.ChatMessageDTO;
import com.project.realtimechat.entity.ChatMessage;
import com.project.realtimechat.entity.EnumMessageType;

public interface ChatMessageService {
	// CRUD Operations
    ResponseEntity<BaseDTO<ChatMessageDTO>> getChatMessageById(Long id);
    ResponseEntity<BaseDTO<List<ChatMessageDTO>>> getChatMessagesByChatRoomId(Long chatRoomId);
    ResponseEntity<BaseDTO<Page<ChatMessageDTO>>> getChatMessagesPaginated(Long chatRoomId, Pageable pageable);
    ResponseEntity<BaseDTO<List<ChatMessageDTO>>> getChatMessagesAfterTimestamp(Long chatRoomId, Instant timestamp);
    ResponseEntity<BaseDTO<ChatMessageDTO>> createTextMessage(Long chatRoomId, Long senderId, String content);
    ResponseEntity<BaseDTO<ChatMessageDTO>> createSystemMessage(Long chatRoomId, String content);
    ResponseEntity<BaseDTO<ChatMessageDTO>> createMessageWithAttachments(
            Long chatRoomId, Long senderId, String content, EnumMessageType type, Set<String> attachmentUrls);
    ResponseEntity<BaseDTO<Void>> deleteChatMessage(Long messageId, Long userId);
    ResponseEntity<BaseDTO<ChatMessageDTO>> editChatMessage(Long messageId, Long userId, String newContent);
    
    // Search Operations
    ResponseEntity<BaseDTO<List<ChatMessageDTO>>> searchMessagesByContent(Long chatRoomId, String searchTerm);
    ResponseEntity<BaseDTO<List<ChatMessageDTO>>> getMessagesByUser(Long chatRoomId, Long userId);
    ResponseEntity<BaseDTO<List<ChatMessageDTO>>> getMessagesByType(Long chatRoomId, EnumMessageType type);
    
    // Status Operations
    ResponseEntity<BaseDTO<ChatMessageDTO>> updateMessageStatus(Long messageId, Long userId, boolean read, boolean delivered);
    
    // Helper Methods
    ChatMessage findEntityByChatMessageId(Long id);
    Long getUnreadMessageCount(Long chatRoomId, Long userId, Instant since);
    boolean isUserAllowedToModifyMessage(Long messageId, Long userId);
}
