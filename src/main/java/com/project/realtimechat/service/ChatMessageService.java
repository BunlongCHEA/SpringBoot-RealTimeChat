package com.project.realtimechat.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.ChatMessageDTO;
import com.project.realtimechat.entity.ChatMessage;
import com.project.realtimechat.entity.EnumMessageType;

public interface ChatMessageService {
	// CRUD Operations
    ResponseEntity<BaseDTO<ChatMessageDTO>> getMessageById(Long id);
    ResponseEntity<BaseDTO<List<ChatMessageDTO>>> getMessagesByChatRoomId(Long chatRoomId, Pageable pageable);
    ResponseEntity<BaseDTO<ChatMessageDTO>> createTextMessage(Long chatRoomId, Long senderId, String content);
    ResponseEntity<BaseDTO<ChatMessageDTO>> createImageMessage(Long chatRoomId, Long senderId, MultipartFile imageFile);
    ResponseEntity<BaseDTO<ChatMessageDTO>> createImageMessageFromUrl(Long chatRoomId, Long senderId, String imageUrl);
    
    // Helper Methods
    ChatMessage findEntityByChatMessageId(Long id);
}
