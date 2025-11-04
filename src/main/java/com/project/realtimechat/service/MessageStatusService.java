package com.project.realtimechat.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.MessageStatusDTO;
import com.project.realtimechat.entity.EnumStatus;

public interface MessageStatusService {
	// CRUD Operations
	ResponseEntity<BaseDTO<MessageStatusDTO>> createMessageStatus(Long userId, Long messageId, EnumStatus status);
    
    ResponseEntity<BaseDTO<MessageStatusDTO>> updateMessageStatus(Long userId, Long messageId, EnumStatus status);
    
    ResponseEntity<BaseDTO<MessageStatusDTO>> getMessageStatusByUserAndMessage(Long userId, Long messageId, boolean isReceivedUser);
 }
