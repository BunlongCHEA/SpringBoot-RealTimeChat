package com.project.realtimechat.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.ChatRoomDTO;
import com.project.realtimechat.service.ChatRoomService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rooms")
public class ChatRoomController {
	@Autowired
	private ChatRoomService chatRoomService;
	
	@GetMapping("/{id}")
	public ResponseEntity<BaseDTO<ChatRoomDTO>> getChatRoomById(@PathVariable Long id) {               
        return chatRoomService.getChatRoomById(id);
    }
	
	@GetMapping("/user/{userId}")
    public ResponseEntity<BaseDTO<List<ChatRoomDTO>>> getChatRoomsByUserId(@PathVariable Long userId) {
        return chatRoomService.getChatRoomsByUserId(userId);
    }
	
	@GetMapping
	public ResponseEntity<BaseDTO<List<ChatRoomDTO>>> getAllChatRooms() { 
        return chatRoomService.getAllChatRooms();
    }
	
	@PostMapping
	public ResponseEntity<BaseDTO<ChatRoomDTO>> createChatRoom(
            @Valid @RequestBody ChatRoomDTO chatRoomDTO,
            @RequestParam Long currentUserId) {         
        return chatRoomService.createChatRoom(chatRoomDTO, currentUserId);
    }
	
	@PutMapping("/{id}")
	public ResponseEntity<BaseDTO<ChatRoomDTO>> updateChatRoom(
            @PathVariable Long id,
            @Valid @RequestBody ChatRoomDTO chatRoomDTO,
            @RequestParam Long currentUserId) {     
        return chatRoomService.updateChatRoom(id, chatRoomDTO, currentUserId);
    }
	
	@DeleteMapping("/{id}")
	public ResponseEntity<BaseDTO<Void>> deleteChatRoom(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") Boolean deleteForAll) {
        return chatRoomService.deleteChatRoom(id, userId, deleteForAll);
    }
}
