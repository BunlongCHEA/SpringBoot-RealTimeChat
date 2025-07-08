package com.project.realtimechat.serviceImpl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.ChatMessageDTO;
import com.project.realtimechat.entity.ChatMessage;
import com.project.realtimechat.entity.ChatRoom;
import com.project.realtimechat.entity.EnumMessageType;
import com.project.realtimechat.entity.EnumRoomRole;
import com.project.realtimechat.entity.EnumStatus;
import com.project.realtimechat.entity.MessageStatus;
import com.project.realtimechat.entity.Participant;
import com.project.realtimechat.entity.User;
import com.project.realtimechat.exception.BadRequestException;
import com.project.realtimechat.exception.ResourceNotFoundException;
import com.project.realtimechat.repository.ChatMessageRepository;
import com.project.realtimechat.repository.ChatRoomRepository;
import com.project.realtimechat.repository.MessageStatusRepository;
import com.project.realtimechat.repository.ParticipantRepository;
import com.project.realtimechat.service.ChatMessageService;
import com.project.realtimechat.service.ChatRoomService;
import com.project.realtimechat.service.UserService;

/**
 * Implementation of ChatMessageService that provides message management,
 * search, and status operations for the real-time chat application.
 */
@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    
    @Autowired
    private ParticipantRepository participantRepository;
    
    @Autowired
    private MessageStatusRepository messageStatusRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ChatRoomService chatRoomService;
    
    @Autowired
    private ModelMapper modelMapper;

    /**
     * Retrieves a chat message by its ID
     * @param id The ID of the message to retrieve
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<ChatMessageDTO>> getChatMessageById(Long id) {
        try {
            ChatMessage chatMessage = findEntityByChatMessageId(id);
            ChatMessageDTO chatMessageDTO = convertToDTO(chatMessage);
            
            BaseDTO<ChatMessageDTO> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Message retrieved successfully", chatMessageDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to retrieve message: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves all messages in a chat room
     * @param chatRoomId The ID of the chat room
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<List<ChatMessageDTO>>> getChatMessagesByChatRoomId(Long chatRoomId) {
        try {
            // Verify the chat room exists
            chatRoomService.findEntityByChatRoomId(chatRoomId);
            
            List<ChatMessage> messages = chatMessageRepository.findByChatRoomsId(chatRoomId);
            List<ChatMessageDTO> messageDTOs = messages.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            BaseDTO<List<ChatMessageDTO>> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Messages retrieved successfully", messageDTOs);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to retrieve messages: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves messages with pagination
     * @param chatRoomId The ID of the chat room
     * @param pageable Pagination information
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<Page<ChatMessageDTO>>> getChatMessagesPaginated(
            Long chatRoomId, Pageable pageable) {
        try {
            // Verify the chat room exists
            chatRoomService.findEntityByChatRoomId(chatRoomId);
            
            Page<ChatMessage> messagePage = chatMessageRepository.findByChatRoomsIdOrderByTimestampDesc(
                    chatRoomId, pageable);
            
            List<ChatMessageDTO> messageDTOs = messagePage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Page<ChatMessageDTO> dtoPage = new PageImpl<>(
                    messageDTOs, pageable, messagePage.getTotalElements());
            
            BaseDTO<Page<ChatMessageDTO>> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Messages retrieved successfully", dtoPage);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to retrieve messages: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves messages sent after a specific timestamp
     * @param chatRoomId The ID of the chat room
     * @param timestamp Messages after this timestamp will be retrieved
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<List<ChatMessageDTO>>> getChatMessagesAfterTimestamp(
            Long chatRoomId, Instant timestamp) {
        try {
            // Verify the chat room exists
            chatRoomService.findEntityByChatRoomId(chatRoomId);
            
            List<ChatMessage> messages = chatMessageRepository
                    .findByChatRoomsIdAndTimestampAfterOrderByTimestampAsc(chatRoomId, timestamp);
            
            List<ChatMessageDTO> messageDTOs = messages.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            BaseDTO<List<ChatMessageDTO>> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Messages retrieved successfully", messageDTOs);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to retrieve messages: " + e.getMessage());
        }
    }
    
    /**
     * Creates a new text message
     * @param chatRoomId The ID of the chat room
     * @param senderId The ID of the sender
     * @param content The message content
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<ChatMessageDTO>> createTextMessage(
            Long chatRoomId, Long senderId, String content) {
        try {
            // Verify the chat room exists
            ChatRoom chatRoom = chatRoomService.findEntityByChatRoomId(chatRoomId);
            
            // Verify the sender exists and is a participant
            User sender = userService.findEntityByIdUsers(senderId);
            
            if (!participantRepository.findByUsersIdAndChatRoomsId(senderId, chatRoomId).isPresent()) {
                throw new BadRequestException("User is not a participant in this chat room");
            }
            
            // Check if the participant is muted
            Participant participant = participantRepository.findByUsersIdAndChatRoomsId(senderId, chatRoomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
            
            if (participant.isMuted()) {
                throw new BadRequestException("You are muted in this chat room");
            }
            
            // Create the message
            ChatMessage message = new ChatMessage();
            message.setChatRooms(chatRoom);
            message.setSender(sender);
            message.setContent(content);
            message.setType(EnumMessageType.TEXT);
            message.setTimestamp(Instant.now());
            message.setAttachmentUrls(new HashSet<>());
            
            ChatMessage savedMessage = chatMessageRepository.save(message);
            
            // Update the chat room's last message
            chatRoom.setChatMessages(savedMessage);
            chatRoomRepository.save(chatRoom);
            
            ChatMessageDTO messageDTO = convertToDTO(savedMessage);
            
            BaseDTO<ChatMessageDTO> response = new BaseDTO<>(HttpStatus.CREATED.value(), 
                    "Message created successfully", messageDTO);
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to create message: " + e.getMessage());
        }
    }
    
    /**
     * Creates a new system message
     * @param chatRoomId The ID of the chat room
     * @param content The system message content
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<ChatMessageDTO>> createSystemMessage(
            Long chatRoomId, String content) {
        try {
            // Verify the chat room exists
            ChatRoom chatRoom = chatRoomService.findEntityByChatRoomId(chatRoomId);
            
            // Create the message
            ChatMessage message = new ChatMessage();
            message.setChatRooms(chatRoom);
            message.setContent(content);
            message.setType(EnumMessageType.SYSTEM);
            message.setTimestamp(Instant.now());
            message.setAttachmentUrls(new HashSet<>());
            
            ChatMessage savedMessage = chatMessageRepository.save(message);
            
            // Update the chat room's last message
            chatRoom.setChatMessages(savedMessage);
            chatRoomRepository.save(chatRoom);
            
            ChatMessageDTO messageDTO = convertToDTO(savedMessage);
            
            BaseDTO<ChatMessageDTO> response = new BaseDTO<>(HttpStatus.CREATED.value(), 
                    "System message created successfully", messageDTO);
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to create system message: " + e.getMessage());
        }
    }
    
    /**
     * Creates a new message with attachments (images or files)
     * @param chatRoomId The ID of the chat room
     * @param senderId The ID of the sender
     * @param content The message content (optional for attachments)
     * @param type The type of message (IMAGE, FILE)
     * @param attachmentUrls URLs of the attachments
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<ChatMessageDTO>> createMessageWithAttachments(
            Long chatRoomId, Long senderId, String content, EnumMessageType type, Set<String> attachmentUrls) {
        try {
            // Verify valid message type
            if (type != EnumMessageType.IMAGE && type != EnumMessageType.FILE) {
                throw new BadRequestException("Invalid message type for attachments");
            }
            
            // Verify attachments are provided
            if (attachmentUrls == null || attachmentUrls.isEmpty()) {
                throw new BadRequestException("Attachments are required");
            }
            
            // Verify the chat room exists
            ChatRoom chatRoom = chatRoomService.findEntityByChatRoomId(chatRoomId);
            
            // Verify the sender exists and is a participant
            User sender = userService.findEntityByIdUsers(senderId);
            
            if (!participantRepository.findByUsersIdAndChatRoomsId(senderId, chatRoomId).isPresent()) {
                throw new BadRequestException("User is not a participant in this chat room");
            }
            
            // Check if the participant is muted
            Participant participant = participantRepository.findByUsersIdAndChatRoomsId(senderId, chatRoomId)
                    .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
            
            if (participant.isMuted()) {
                throw new BadRequestException("You are muted in this chat room");
            }
            
            // Create the message
            ChatMessage message = new ChatMessage();
            message.setChatRooms(chatRoom);
            message.setSender(sender);
            message.setContent(content);
            message.setType(type);
            message.setTimestamp(Instant.now());
            message.setAttachmentUrls(attachmentUrls);
            
            ChatMessage savedMessage = chatMessageRepository.save(message);
            
            // Update the chat room's last message
            chatRoom.setChatMessages(savedMessage);
            chatRoomRepository.save(chatRoom);
            
            ChatMessageDTO messageDTO = convertToDTO(savedMessage);
            
            BaseDTO<ChatMessageDTO> response = new BaseDTO<>(HttpStatus.CREATED.value(), 
                    "Message with attachments created successfully", messageDTO);
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to create message: " + e.getMessage());
        }
    }
    
    /**
     * Deletes a chat message
     * Only the sender or an admin can delete messages
     * @param messageId The ID of the message to delete
     * @param userId The ID of the user performing the action
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<Void>> deleteChatMessage(Long messageId, Long userId) {
        try {
            // Find the message
            ChatMessage message = findEntityByChatMessageId(messageId);
            ChatRoom chatRoom = message.getChatRooms();
            
            // Check permission - must be sender or admin
            boolean isSender = message.getSender() != null && message.getSender().getId().equals(userId);
            boolean isAdmin = participantRepository.findByUsersIdAndChatRoomsId(userId, chatRoom.getId())
                    .map(p -> p.getRole() == EnumRoomRole.ADMIN)
                    .orElse(false);
                    
            if (!isSender && !isAdmin) {
                throw new BadRequestException("You don't have permission to delete this message");
            }
            
            // Check if this is the chat room's last message
            boolean isLastMessage = chatRoom.getChatMessages() != null && 
                                   chatRoom.getChatMessages().getId().equals(messageId);
                    
            // Delete the message
            chatMessageRepository.delete(message);
            
            // If this was the last message, update the chat room's last message
            if (isLastMessage) {
                // Find the new last message
                List<ChatMessage> messages = chatMessageRepository.findByChatRoomsId(chatRoom.getId());
                if (!messages.isEmpty()) {
                    // Sort by timestamp descending
                    ChatMessage newLastMessage = messages.stream()
                            .max((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
                            .orElse(null);
                    
                    chatRoom.setChatMessages(newLastMessage);
                } else {
                    chatRoom.setChatMessages(null);
                }
                chatRoomRepository.save(chatRoom);
            }
            
            BaseDTO<Void> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Message deleted successfully", null);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to delete message: " + e.getMessage());
        }
    }
    
    /**
     * Edits a chat message's content
     * Only the sender can edit messages
     * @param messageId The ID of the message to edit
     * @param userId The ID of the user performing the action (must be the sender)
     * @param newContent The new message content
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<ChatMessageDTO>> editChatMessage(
            Long messageId, Long userId, String newContent) {
        try {
            // Find the message
            ChatMessage message = findEntityByChatMessageId(messageId);
            
            // Only the sender can edit messages
            if (message.getSender() == null || !message.getSender().getId().equals(userId)) {
                throw new BadRequestException("Only the sender can edit this message");
            }
            
            // Only text messages can be edited
            if (message.getType() != EnumMessageType.TEXT) {
                throw new BadRequestException("Only text messages can be edited");
            }
            
            // Update the content
            message.setContent(newContent);
            ChatMessage updatedMessage = chatMessageRepository.save(message);
            
            ChatMessageDTO messageDTO = convertToDTO(updatedMessage);
            
            BaseDTO<ChatMessageDTO> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Message updated successfully", messageDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to update message: " + e.getMessage());
        }
    }
    
    /**
     * Searches for messages by content in a chat room
     * @param chatRoomId The ID of the chat room
     * @param searchTerm The search term
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<List<ChatMessageDTO>>> searchMessagesByContent(
            Long chatRoomId, String searchTerm) {
        try {
            // Verify the chat room exists
            chatRoomService.findEntityByChatRoomId(chatRoomId);
            
            List<ChatMessage> messages = chatMessageRepository.searchByContent(chatRoomId, searchTerm);
            List<ChatMessageDTO> messageDTOs = messages.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            BaseDTO<List<ChatMessageDTO>> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Messages retrieved successfully", messageDTOs);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to search messages: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves messages sent by a specific user in a chat room
     * @param chatRoomId The ID of the chat room
     * @param userId The ID of the user
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<List<ChatMessageDTO>>> getMessagesByUser(Long chatRoomId, Long userId) {
        try {
            // Verify the chat room exists
            chatRoomService.findEntityByChatRoomId(chatRoomId);
            
            // Verify the user exists
            userService.findEntityByIdUsers(userId);
            
            List<ChatMessage> messages = chatMessageRepository.findByChatRoomsIdAndSenderId(chatRoomId, userId);
            List<ChatMessageDTO> messageDTOs = messages.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            BaseDTO<List<ChatMessageDTO>> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Messages retrieved successfully", messageDTOs);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to retrieve messages: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves messages of a specific type in a chat room
     * @param chatRoomId The ID of the chat room
     * @param type The message type
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<List<ChatMessageDTO>>> getMessagesByType(
            Long chatRoomId, EnumMessageType type) {
        try {
            // Verify the chat room exists
            chatRoomService.findEntityByChatRoomId(chatRoomId);
            
            List<ChatMessage> messages = chatMessageRepository.findByChatRoomsIdAndType(chatRoomId, type);
            List<ChatMessageDTO> messageDTOs = messages.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            BaseDTO<List<ChatMessageDTO>> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Messages retrieved successfully", messageDTOs);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to retrieve messages: " + e.getMessage());
        }
    }
    
    /**
     * Updates a message's status (read/delivered) for a specific user
     * @param messageId The ID of the message
     * @param userId The ID of the user
     * @param read Whether the message is read
     * @param delivered Whether the message is delivered
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<ChatMessageDTO>> updateMessageStatus(
            Long messageId, Long userId, boolean read, boolean delivered) {
        try {
            // Find the message
            ChatMessage message = findEntityByChatMessageId(messageId);
            
            // Verify the user exists and is a participant
            User user = userService.findEntityByIdUsers(userId);
            
            if (!participantRepository.findByUsersIdAndChatRoomsId(userId, message.getChatRooms().getId())
                    .isPresent()) {
                throw new BadRequestException("User is not a participant in this chat room");
            }
            
            // Update or create the message status
            MessageStatus status = messageStatusRepository
                    .findByUsersIdAndChatMessagesId(messageId, userId)
                    .orElse(new MessageStatus());
                    
            status.setChatMessages(message);
            status.setUsers(user);
            
            // Find the appropriate status based on read/delivered params
            EnumStatus newStatus;
            if (read) {
                newStatus = EnumStatus.READ;
            } else if (delivered) {
                newStatus = EnumStatus.DELIVERED;
            } else {
                newStatus = EnumStatus.SENT;
            }
            
            status.setStatus(newStatus);
            status.setTimestamp(Instant.now());
            
            messageStatusRepository.save(status);
            
            // If this status is read, update the participant's last read message ID
            if (read) {
                participantRepository.findByUsersIdAndChatRoomsId(userId, message.getChatRooms().getId())
                    .ifPresent(p -> {
                        p.setLastReadMessageId(messageId);
                        participantRepository.save(p);
                    });
            }
            
            ChatMessageDTO messageDTO = convertToDTO(message);
            
            BaseDTO<ChatMessageDTO> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Message status updated successfully", messageDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to update message status: " + e.getMessage());
        }
    }
    
    /**
     * Finds a chat message entity by ID
     * @param id The ID of the message
     */
    @Override
    @Transactional(readOnly = true)
    public ChatMessage findEntityByChatMessageId(Long id) {
        try {
            return chatMessageRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Message not found with ID: " + id));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to find message: " + e.getMessage());
        }
    }
    
    /**
     * Gets the count of unread messages in a chat room for a user
     * @param chatRoomId The ID of the chat room
     * @param userId The ID of the user
     * @param since Only count messages after this timestamp
     */
    @Override
    @Transactional(readOnly = true)
    public Long getUnreadMessageCount(Long chatRoomId, Long userId, Instant since) {
        try {
            // Verify the chat room exists
            chatRoomService.findEntityByChatRoomId(chatRoomId);
            
            // Verify the user exists and is a participant
            userService.findEntityByIdUsers(userId);
            
            if (!participantRepository.findByUsersIdAndChatRoomsId(userId, chatRoomId).isPresent()) {
                throw new BadRequestException("User is not a participant in this chat room");
            }
            
            return chatMessageRepository.countUnreadMessages(chatRoomId, userId, since);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to count unread messages: " + e.getMessage());
        }
    }
    
    /**
     * Checks if a user is allowed to modify a message
     * @param messageId The ID of the message
     * @param userId The ID of the user
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isUserAllowedToModifyMessage(Long messageId, Long userId) {
        try {
            ChatMessage message = findEntityByChatMessageId(messageId);
            
            // Check if user is the sender
            boolean isSender = message.getSender() != null && 
                              message.getSender().getId().equals(userId);
            
            // Check if user is an ADMIN in the chat room
            boolean isAdmin = participantRepository
                    .findByUsersIdAndChatRoomsId(userId, message.getChatRooms().getId())
                    .map(p -> p.getRole() == EnumRoomRole.ADMIN)
                    .orElse(false);
                    
            return isSender || isAdmin;
        } catch (Exception e) {
            throw new BadRequestException("Failed to check message modification permission: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to convert a ChatMessage entity to ChatMessageDTO
     * @param message The chat message entity
     */
    private ChatMessageDTO convertToDTO(ChatMessage message) {
        ChatMessageDTO dto = modelMapper.map(message, ChatMessageDTO.class);
        
        // Set sender name if available
        if (message.getSender() != null) {
            dto.setSenderName(message.getSender().getUsername());
        }
        
        return dto;
    }
}