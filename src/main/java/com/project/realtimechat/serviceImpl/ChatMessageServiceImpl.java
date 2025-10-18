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
import org.springframework.web.multipart.MultipartFile;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.ChatMessageDTO;
import com.project.realtimechat.entity.ChatMessage;
import com.project.realtimechat.entity.ChatRoom;
import com.project.realtimechat.entity.EnumMessageType;
import com.project.realtimechat.entity.EnumRoomRole;
import com.project.realtimechat.entity.EnumRoomType;
import com.project.realtimechat.entity.EnumStatus;
import com.project.realtimechat.entity.ImageDocument;
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
import com.project.realtimechat.service.DateSeparatorService;
import com.project.realtimechat.service.ImageService;
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
    private DateSeparatorService dateSeparatorService;
    
    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private ImageService imageService;
    
//    private static final String utcString = Instant.now().toString();

    /**
     * Retrieves a message by its ID
     * @param id The ID of the message
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<ChatMessageDTO>> getMessageById(Long id) {
        try {
            ChatMessage message = findEntityByChatMessageId(id);
            ChatMessageDTO messageDTO =  modelMapper.map(message, ChatMessageDTO.class);
            
            BaseDTO<ChatMessageDTO> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Message retrieved successfully", messageDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to retrieve message: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves messages for a chat room with pagination
     * @param chatRoomId The ID of the chat room
     * @param pageable Pagination information
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<List<ChatMessageDTO>>> getMessagesByChatRoomId(
            Long chatRoomId, Pageable pageable) {
        try {
        	// Verify the chat room exists
            ChatRoom chatRoom = chatRoomService.findEntityByChatRoomId(chatRoomId);
            
            Page<ChatMessage> messagePage = chatMessageRepository.findByChatRoomsIdOrderByTimestampDesc(
                    chatRoomId, pageable);
            
            List<ChatMessage> messages = messagePage.getContent();
            
            // Insert smart date separators for personal chats
            if (chatRoom.getType() == EnumRoomType.PERSONAL) {
                messages = dateSeparatorService.insertDateSeparators(messages);
            }
            
            List<ChatMessageDTO> messageDTOs = messages.stream()
                    .map(message -> modelMapper.map(message, ChatMessageDTO.class))
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
            
            ChatMessageDTO messageDTO = modelMapper.map(savedMessage, ChatMessageDTO.class);
            
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
     * Creates a new image for message : Upload and send image messages via button
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<ChatMessageDTO>> createImageMessage(
            Long chatRoomId, Long senderId, MultipartFile imageFile) {
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
            
            // Create the message first
            ChatMessage message = new ChatMessage();
            message.setChatRooms(chatRoom);
            message.setSender(sender);
            message.setContent(""); // Empty content for image messages
            message.setType(EnumMessageType.IMAGE);
            message.setTimestamp(Instant.now());
            message.setAttachmentUrls(new HashSet<>());
            
            ChatMessage savedMessage = chatMessageRepository.save(message);
            
            // Save image to MongoDB
            ImageDocument savedImage = imageService.saveImage(
                imageFile, 
                sender.getUsername(), 
                chatRoomId, 
                savedMessage.getId()
            );
            
            // Add image URL to message attachments
            String imageUrl = imageService.generateImageUrl(savedImage.getId());
            savedMessage.getAttachmentUrls().add(imageUrl);
            savedMessage = chatMessageRepository.save(savedMessage);
            
            // Update the chat room's last message
            chatRoom.setChatMessages(savedMessage);
            chatRoomRepository.save(chatRoom);
            
            ChatMessageDTO messageDTO = modelMapper.map(savedMessage, ChatMessageDTO.class);
            
            BaseDTO<ChatMessageDTO> response = new BaseDTO<>(HttpStatus.CREATED.value(), 
                    "Image message created successfully", messageDTO);
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
            
    	} catch (ResourceNotFoundException | BadRequestException e) {
    		throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to create image message: " + e.getMessage());
        }
    }
    
    /**
     * Creates a new image for message : Send image messages from existing URLs
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<ChatMessageDTO>> createImageMessageFromUrl(
            Long chatRoomId, Long senderId, String imageUrl) {
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
            message.setContent(""); // Empty content for image messages
            message.setType(EnumMessageType.IMAGE);
            message.setTimestamp(Instant.now());
            
            // Add image URL to attachments
            Set<String> attachmentUrls = new HashSet<>();
            attachmentUrls.add(imageUrl);
            message.setAttachmentUrls(attachmentUrls);
            
            ChatMessage savedMessage = chatMessageRepository.save(message);
            
            // Update the chat room's last message
            chatRoom.setChatMessages(savedMessage);
            chatRoomRepository.save(chatRoom);
            
            ChatMessageDTO messageDTO = modelMapper.map(savedMessage, ChatMessageDTO.class);
            
            BaseDTO<ChatMessageDTO> response = new BaseDTO<>(HttpStatus.CREATED.value(), 
                    "Image message created successfully", messageDTO);
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to create image message: " + e.getMessage());
        }
    }
    
    /**
     * Finds a chat message entity by ID
     * @param id The ID of the message
     */
    @Override
    @Transactional(readOnly = true)
    public ChatMessage findEntityByChatMessageId(Long id) {
        return chatMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with ID: " + id));
    }
}