package com.project.realtimechat.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.ParticipantDTO;
import com.project.realtimechat.entity.EnumRoomRole;
import com.project.realtimechat.service.ParticipantService;

@RestController
@RequestMapping("/api/participants")
public class ParticipantController {
	@Autowired
	private ParticipantService participantService;
	
	@GetMapping("/{id}")
	public ResponseEntity<BaseDTO<ParticipantDTO>> getParticipantById(@PathVariable Long id) {
        return participantService.getParticipantById(id);
    }
	
	@GetMapping("/room/{chatRoomId}")
	public ResponseEntity<BaseDTO<List<ParticipantDTO>>> getParticipantsByChatRoomId(@PathVariable Long chatRoomId) {
        return participantService.getParticipantsByChatRoomId(chatRoomId);
    }
	
	@GetMapping("/user/{userId}/room/{chatRoomId}")
	public ResponseEntity<BaseDTO<ParticipantDTO>> getParticipantByUserAndChatRoom(
            @PathVariable Long userId, 
            @PathVariable Long chatRoomId) {
        return participantService.getParticipantByUserAndChatRoom(userId, chatRoomId);
    }
	
	@GetMapping("/user/{userId}")
	public ResponseEntity<BaseDTO<List<ParticipantDTO>>> getParticipantsByUserId(
            @PathVariable Long userId) {
        return participantService.getParticipantsByUserId(userId);
    }
	
	@GetMapping("/user/{userId}/chat-partners")
	public ResponseEntity<BaseDTO<List<ParticipantDTO>>> getChatPartners(@PathVariable Long userId) {
	    return participantService.getChatPartners(userId);
	}
	
	@GetMapping("/user/{userId}/personal-chat-partners")
    public ResponseEntity<BaseDTO<List<ParticipantDTO>>> getPersonalChatPartners(@PathVariable Long userId) {
        return participantService.getPersonalChatPartners(userId);
    }
	
	@PostMapping("/room/{chatRoomId}/add")
	public ResponseEntity<BaseDTO<ParticipantDTO>> addParticipantToChatRoom(
            @PathVariable Long chatRoomId,
            @RequestParam Long userId,
            @RequestParam Long addedByUserId) {
        return participantService.addParticipantToChatRoom(chatRoomId, userId, addedByUserId);
    }
	
	@DeleteMapping("/{participantId}")
	public ResponseEntity<BaseDTO<Void>> removeParticipantFromChatRoom(
            @PathVariable Long participantId,
            @RequestParam Long removedByUserId) {
        return participantService.removeParticipantFromChatRoom(participantId, removedByUserId);
    }
	
	@PutMapping("/{participantId}/role")
	public ResponseEntity<BaseDTO<ParticipantDTO>> updateParticipantRole(
            @PathVariable Long participantId,
            @RequestParam EnumRoomRole newRole,
            @RequestParam Long updatedByUserId) {
        return participantService.updateParticipantRole(participantId, newRole, updatedByUserId);
    }
	
	@PutMapping("/{participantId}/status")
	public ResponseEntity<BaseDTO<ParticipantDTO>> updateParticipantStatus(
            @PathVariable Long participantId,
            @RequestParam(required = false) Boolean muted,
            @RequestParam(required = false) Boolean blocked) {
        return participantService.updateParticipantStatus(participantId, muted, blocked);
    }
	
	@PutMapping("/user/{userId}/room/{chatRoomId}/read")
	public ResponseEntity<BaseDTO<ParticipantDTO>> updateLastReadMessageId(
            @PathVariable Long userId,
            @PathVariable Long chatRoomId,
            @RequestParam Long messageId) {
        return participantService.updateLastReadMessageId(userId, chatRoomId, messageId);
    }
	
	@PutMapping("/user/{userId}/online")
	public ResponseEntity<BaseDTO<ParticipantDTO>> updateOnlineStatus(
            @PathVariable Long userId,
            @RequestParam boolean online) {
        return participantService.updateOnlineStatus(userId, online);
    }
	
	@PutMapping("/user/{userId}/lastseen")
	public ResponseEntity<BaseDTO<ParticipantDTO>> updateLastSeen(
            @PathVariable Long userId,
            @RequestParam String lastSeen) {       
        LocalDateTime lastSeenDateTime = LocalDateTime.parse(lastSeen, 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
        return participantService.updateLastSeen(userId, lastSeenDateTime);
    }
}
