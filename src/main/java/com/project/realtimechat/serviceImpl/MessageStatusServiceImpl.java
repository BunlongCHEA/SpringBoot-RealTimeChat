package com.project.realtimechat.serviceImpl;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.realtimechat.config.WebSocketEventPublisher;
import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.MessageStatusDTO;
import com.project.realtimechat.entity.ChatMessage;
import com.project.realtimechat.entity.EnumStatus;
import com.project.realtimechat.entity.MessageStatus;
import com.project.realtimechat.entity.User;
import com.project.realtimechat.exception.BadRequestException;
import com.project.realtimechat.exception.ResourceNotFoundException;
import com.project.realtimechat.repository.ChatMessageRepository;
import com.project.realtimechat.repository.MessageStatusRepository;
import com.project.realtimechat.repository.UserRepository;
import com.project.realtimechat.service.MessageStatusService;

@Service
public class MessageStatusServiceImpl implements MessageStatusService {
	
	private static final Logger log = LoggerFactory.getLogger(MessageStatusServiceImpl.class);
	
	@Autowired
    private MessageStatusRepository messageStatusRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private WebSocketEventPublisher webSocketEventPublisher;
    
    /**
     * Creates a new message status for a user and message
     * @param userId The ID of the user
     * @param messageId The ID of the message
     * @param status The status to set
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<MessageStatusDTO>> createMessageStatus(Long userId, Long messageId, EnumStatus status) {
        try {
            // Validate inputs
//            if (userId == null || messageId == null || status == null) {
//                throw new BadRequestException("User ID, Message ID, and Status are required");
//            }
            
            // Fetch user and chat message
            User user = findEntityByUserId(userId);
            ChatMessage chatMessage = findEntityByMessageId(messageId);
            
            // Check if message status already exists
            messageStatusRepository.findByUsersReceivedIdAndChatMessagesId(userId, messageId)
                    .ifPresent(existingStatus -> {
                        throw new BadRequestException("Message status already exists for this user and message");
                    });
            
            // Create new message status
            MessageStatus messageStatus = new MessageStatus();
            messageStatus.setUsersReceived(user);
            
            messageStatus.setUsersSent(chatMessage.getSender());
            messageStatus.setChatMessages(chatMessage);
            messageStatus.setStatus(status);
            messageStatus.setTimestamp(Instant.now());
            
            MessageStatus savedMessageStatus = messageStatusRepository.save(messageStatus);
            
            // Convert to DTO
            MessageStatusDTO messageStatusDTO = modelMapper.map(savedMessageStatus, MessageStatusDTO.class);
            
            if (chatMessage != null) {
                webSocketEventPublisher.broadcastMessageStatus(messageId, userId, status, chatMessage.getChatRooms().getId());
            }
            
            BaseDTO<MessageStatusDTO> response = new BaseDTO<>(
                HttpStatus.CREATED.value(),
                "Message status created successfully",
                messageStatusDTO
            );
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
            
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to create message status: " + e.getMessage());
        }
    }
    
    /**
     * Updates an existing message status
     * @param userId The ID of the user
     * @param messageId The ID of the message
     * @param status The new status to set
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<MessageStatusDTO>> updateMessageStatus(Long userId, Long messageId, EnumStatus status) {
        try {
            // Validate inputs
//            if (userId == null || messageId == null || status == null) {
//                throw new BadRequestException("User ID, Message ID, and Status are required");
//            }
            
            // Fetch user and chat message to ensure they exist
            User user = findEntityByUserId(userId);
            ChatMessage chatMessage = findEntityByMessageId(messageId);
            
            log.debug("[{}] | Checking User ID {} with Chat Message ID {}", 
            		Instant.now(), userId, messageId);
            
            
            // Find the existing message status
            MessageStatus messageStatus = messageStatusRepository.findByUsersReceivedIdAndChatMessagesId(userId, messageId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Message status not found for user ID: " + userId + " and message ID: " + messageId));
            
            // Validate status transition
//            validateStatusTransition(messageStatus.getStatus(), status);
            
            // Update message status
            messageStatus.setStatus(status);
            messageStatus.setTimestamp(Instant.now());
            
            MessageStatus updatedMessageStatus = messageStatusRepository.save(messageStatus);
            
            // Convert to DTO
            MessageStatusDTO messageStatusDTO = modelMapper.map(updatedMessageStatus, MessageStatusDTO.class);
            
            if (chatMessage != null) {
                webSocketEventPublisher.broadcastMessageStatus(messageId, userId, status, chatMessage.getChatRooms().getId());
            }
            
            BaseDTO<MessageStatusDTO> response = new BaseDTO<>(
                HttpStatus.OK.value(),
                "Message status updated successfully",
                messageStatusDTO
            );
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to update message status: " + e.getMessage());
        }
    }
    
    /**
     * Gets a message status for a specific received user and message
     * @param userId The ID of the received user
     * @param messageId The ID of the message
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<MessageStatusDTO>> getMessageStatusByUserAndMessage(Long userId, Long messageId, boolean isReceivedUser) {
        try {
            // Validate inputs
//            if (userId == null || messageId == null) {
//                throw new BadRequestException("User ID and Message ID are required");
//            }
            
            // Find the message status
            MessageStatus messageStatus = findMessageStatusByUserAndMessage(userId, messageId, isReceivedUser);
            
            // Convert to DTO
            MessageStatusDTO messageStatusDTO = modelMapper.map(messageStatus, MessageStatusDTO.class);
            
            BaseDTO<MessageStatusDTO> response = new BaseDTO<>(
                HttpStatus.OK.value(),
                "Message status retrieved successfully",
                messageStatusDTO
            );
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to get message status: " + e.getMessage());
        }
    }
    
    
    /**
     * Helper method to get message status entity by user and message
     * @param userId The ID of the user
     * @param messageId The ID of the message
     */
    private MessageStatus findMessageStatusByUserAndMessage(Long userId, Long messageId, boolean isReceivedUser) {
//        return messageStatusRepository.findByUsersReceivedIdAndChatMessagesId(userId, messageId)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        "Message status not found for user ID: " + userId + " and message ID: " + messageId));
    	
    	if (isReceivedUser) {
            return messageStatusRepository.findByUsersReceivedIdAndChatMessagesId(userId, messageId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Message status not found for RECEIVED user ID: " + userId + " and message ID: " + messageId));
        } else {
            return messageStatusRepository.findByUsersSentIdAndChatMessagesId(userId, messageId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Message status not found for SENT user ID: " + userId + " and message ID: " + messageId));
        }
    }
    
    /**
     * Helper method to find a user entity by ID
     * @param userId The ID of the user
     */
    private User findEntityByUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }
    
    /**
     * Helper method to find a chat message entity by ID
     * @param messageId The ID of the chat message
     */
    private ChatMessage findEntityByMessageId(Long messageId) {
        return chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with ID: " + messageId));
    }
    
    /**
     * Helper method to validate status transition
     * @param currentStatus The current status
     * @param newStatus The new status
     */
    private void validateStatusTransition(EnumStatus currentStatus, EnumStatus newStatus) {
        // Status can only progress from SENT -> DELIVERED -> READ, not backward
        if (currentStatus == EnumStatus.READ && (newStatus == EnumStatus.SENT || newStatus == EnumStatus.DELIVERED)) {
            throw new BadRequestException("Cannot change status from READ to " + newStatus);
        }
        
        if (currentStatus == EnumStatus.DELIVERED && newStatus == EnumStatus.SENT) {
            throw new BadRequestException("Cannot change status from DELIVERED to SENT");
        }
    }
}
