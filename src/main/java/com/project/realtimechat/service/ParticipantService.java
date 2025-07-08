package com.project.realtimechat.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.ParticipantDTO;
import com.project.realtimechat.entity.EnumRoomRole;
import com.project.realtimechat.entity.Participant;

public interface ParticipantService {
	// CRUD Operations
    ResponseEntity<BaseDTO<ParticipantDTO>> getParticipantById(Long id);
    ResponseEntity<BaseDTO<List<ParticipantDTO>>> getParticipantsByChatRoomId(Long chatRoomId);
    ResponseEntity<BaseDTO<ParticipantDTO>> getParticipantByUserAndChatRoom(Long userId, Long chatRoomId);
    ResponseEntity<BaseDTO<List<ParticipantDTO>>> getParticipantsByUserId(Long userId);
    
    // Participant Management
    ResponseEntity<BaseDTO<ParticipantDTO>> addParticipantToChatRoom(Long chatRoomId, Long userId, Long addedByUserId);
    ResponseEntity<BaseDTO<Void>> removeParticipantFromChatRoom(Long participantId, Long removedByUserId);
    ResponseEntity<BaseDTO<ParticipantDTO>> updateParticipantRole(Long participantId, EnumRoomRole newRole, Long updatedByUserId);
    
    // Status Management
    ResponseEntity<BaseDTO<ParticipantDTO>> updateParticipantStatus(Long participantId, Boolean muted, Boolean blocked);
    ResponseEntity<BaseDTO<ParticipantDTO>> updateLastReadMessageId(Long userId, Long chatRoomId, Long messageId);
    ResponseEntity<BaseDTO<ParticipantDTO>> updateOnlineStatus(Long userId, boolean online);
    ResponseEntity<BaseDTO<ParticipantDTO>> updateLastSeen(Long userId, LocalDateTime lastSeen);
    
    // Helper methods
    Participant findEntityByParticipantId(Long id);
    boolean isUserAdminInChatRoom(Long userId, Long chatRoomId);
    boolean isUserInChatRoom(Long userId, Long chatRoomId);
}
