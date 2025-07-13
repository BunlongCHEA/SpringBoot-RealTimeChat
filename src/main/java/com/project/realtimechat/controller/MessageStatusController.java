package com.project.realtimechat.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.MessageStatusDTO;
import com.project.realtimechat.entity.EnumStatus;
import com.project.realtimechat.service.MessageStatusService;

@RestController
@RequestMapping("/api/messagestatus")
public class MessageStatusController {
	@Autowired
	private MessageStatusService messageStatusService;
	
	@PostMapping
	public ResponseEntity<BaseDTO<MessageStatusDTO>> createMessageStatus(
            @RequestParam Long userId,
            @RequestParam Long messageId,
            @RequestParam EnumStatus status) {
        return messageStatusService.createMessageStatus(userId, messageId, status);
    }
	
	@PutMapping
	public ResponseEntity<BaseDTO<MessageStatusDTO>> updateMessageStatus(
            @RequestParam Long userId,
            @RequestParam Long messageId,
            @RequestParam EnumStatus status) {  
        return messageStatusService.updateMessageStatus(userId, messageId, status);
    }
	
	@GetMapping("/user/{userId}/message/{messageId}")
	public ResponseEntity<BaseDTO<MessageStatusDTO>> getMessageStatusByUserAndMessage(
            @PathVariable Long userId,
            @PathVariable Long messageId) {   
        return messageStatusService.getMessageStatusByUserAndMessage(userId, messageId);
    }
}
