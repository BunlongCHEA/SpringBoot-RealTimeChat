package com.project.realtimechat.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.ChatRoomDTO;
import com.project.realtimechat.entity.ChatMessage;
import com.project.realtimechat.entity.ChatRoom;
import com.project.realtimechat.entity.User;

public interface ChatRoomService {
	// CRUD Operations
	ResponseEntity<BaseDTO<ChatRoomDTO>> getChatRoomById(Long id);
	ResponseEntity<BaseDTO<List<ChatRoomDTO>>> getChatRoomsByUserId(Long userId);
    ResponseEntity<BaseDTO<List<ChatRoomDTO>>> getAllChatRooms();
    ResponseEntity<BaseDTO<ChatRoomDTO>> createChatRoom(ChatRoomDTO chatRoomDTO, Long currentUserId);
    ResponseEntity<BaseDTO<ChatRoomDTO>> updateChatRoom(Long id, ChatRoomDTO chatRoomDTO, Long currentUserId);
    ResponseEntity<BaseDTO<Void>> deleteChatRoom(Long id, Long userId, Boolean deleteForAll);
//    ResponseEntity<BaseDTO<Void>> assignAdmin(Long chatRoomId, Long currentUserId, Long newAdminUserId);
    
    // Helper Methods
    ChatRoom findEntityByChatRoomId(Long id);
    ChatMessage createSystemMessage(ChatRoom chatRoom, User user, String content);
}
