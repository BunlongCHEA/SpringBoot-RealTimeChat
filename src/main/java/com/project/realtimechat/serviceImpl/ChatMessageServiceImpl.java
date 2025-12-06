package com.project.realtimechat.serviceImpl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.project.realtimechat.controller.ChatMessageController;
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
import com.project.realtimechat.service.PushNotificationService;
import com.project.realtimechat.service.UserService;

/**
 * Implementation of ChatMessageService that provides message management,
 * search, and status operations for the real-time chat application.
 */
@Service
public class ChatMessageServiceImpl implements ChatMessageService {
    private static final Logger log = LoggerFactory.getLogger(ChatMessageServiceImpl.class);

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    
    @Autowired
    private ParticipantRepository participantRepository;
    
    @Autowired
    private MessageStatusRepository messageStatusRepository;
    
    @Autowired
    private PushNotificationService pushNotificationService;
    
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
            
            Page<ChatMessage> messagePage = chatMessageRepository.findByChatRoomsIdOrderByTimestampAsc(
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
            
            // Extract URLs from content
            Set<String> extractedUrls = extractUrls(content);
            Set<String> imageUrls = new HashSet<>();
            Set<String> otherUrls = new HashSet<>();

            // Categorize URLs
            for (String url : extractedUrls) {
                if (isImageUrl(url)) {
                    imageUrls.add(url);
                } else {
                    otherUrls.add(url);
                }
            }

            // Determine message type
            EnumMessageType messageType = EnumMessageType.TEXT;
            String messageContent = content;
            Set<String> attachmentUrls = new HashSet<>();

            // Case 1: Only image URL(s), no other text
            if (!imageUrls.isEmpty() && content.trim().equals(String.join(" ", imageUrls))) {
                messageType = EnumMessageType.IMAGE;
                messageContent = "Photo"; // Default content for image-only messages
                attachmentUrls.addAll(imageUrls);
            }
            // Case 2: Image URL(s) with text
            else if (!imageUrls.isEmpty()) {
                messageType = EnumMessageType.TEXT; // Keep as TEXT since there's additional content
                messageContent = content; // Keep full content including URLs
                attachmentUrls.addAll(imageUrls);
                attachmentUrls.addAll(otherUrls);
            }
            // Case 3: Regular URL(s) without images
            else if (!otherUrls.isEmpty()) {
                messageType = EnumMessageType.TEXT;
                messageContent = content;
                attachmentUrls.addAll(otherUrls);
            }
            // Case 4: Plain text, no URLs
            else {
                messageType = EnumMessageType.TEXT;
                messageContent = content;
            }

            // Create the message
            ChatMessage message = new ChatMessage();
            message.setChatRooms(chatRoom);
            message.setSender(sender);
            message.setContent(messageContent);
            message.setType(messageType);
            message.setTimestamp(Instant.now());
            message.setAttachmentUrls(attachmentUrls);
            
            ChatMessage savedMessage = chatMessageRepository.save(message);
            log.info("Created message - ID: {}, Type: {}, HasAttachments: {}, ImageURLs: {}, OtherURLs: {}", 
                savedMessage.getId(), 
                messageType, 
                !attachmentUrls.isEmpty(),
                imageUrls.size(),
                otherUrls.size());
            
            // // Update the chat room's last message
            // chatRoom.setChatMessages(savedMessage);
            // chatRoomRepository.save(chatRoom);

            // Use custom update query instead of full entity save
            chatRoomRepository.updateLastMessageId(chatRoomId, savedMessage.getId());
            log.info("Updated last message for chat room {}", chatRoomId);

            // Send push notifications to users who haven't read the message
            sendPushNotificationsToUnreadUsers(savedMessage);
            
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

            // Save image to MongoDB FIRST
            ImageDocument savedImage = imageService.saveImage(
                imageFile, 
                sender.getUsername(), 
                chatRoomId, 
                null // messageId is null initially, will update later
            );
            log.info("Saved image to MongoDB with ID: {}", savedImage.getId());
            
            // Create the message first
            ChatMessage message = new ChatMessage();
            message.setChatRooms(chatRoom);
            message.setSender(sender);
            message.setContent("Photo"); // Empty content for image messages
            message.setType(EnumMessageType.IMAGE);
            message.setTimestamp(Instant.now());
            // message.setAttachmentUrls(new HashSet<>());
            message.setImageId(savedImage.getId()); // Store MongoDB image ID

            // Generate image URL and add to attachments
            String imageUrl = imageService.generateImageUrl(savedImage.getId());
            Set<String> attachmentUrls = new HashSet<>();
            attachmentUrls.add(imageUrl);
            message.setAttachmentUrls(attachmentUrls);
            
            ChatMessage savedMessage = chatMessageRepository.save(message);
            log.info("Created chat message with ID: {} for room: {} with imageId: {}", savedMessage.getId(), chatRoomId, savedImage.getId());
            
            // // Save image to MongoDB
            // ImageDocument savedImage = imageService.saveImage(
            //     imageFile, 
            //     sender.getUsername(), 
            //     chatRoomId, 
            //     savedMessage.getId()
            // );
            // log.info("Saved image to MongoDB with ID: {}", savedImage.getId());
            
            // // Add image URL to message attachments
            // String imageUrl = imageService.generateImageUrl(savedImage.getId());
            // savedMessage.getAttachmentUrls().add(imageUrl);
            // savedMessage = chatMessageRepository.save(savedMessage);
            // log.info("Updated message with image URL: {}", imageUrl);

            // Update ImageDocument with the messageId
            savedImage.setMessageId(savedMessage.getId());
            imageService.updateImage(savedImage); // Method to update existing image document
            log.info("Linked image {} to message {}", savedImage.getId(), savedMessage.getId());

            // // Update the chat room's last message
            // chatRoom.setChatMessages(savedMessage);
            // chatRoomRepository.save(chatRoom);

            // Use custom update query instead of full entity save
            chatRoomRepository.updateLastMessageId(chatRoomId, savedMessage.getId());
            log.info("Updated last message for chat room {}", chatRoomId);

            // Send push notifications to users who haven't read the message
            sendPushNotificationsToUnreadUsers(savedMessage);
            
            ChatMessageDTO messageDTO = modelMapper.map(savedMessage, ChatMessageDTO.class);
            
            BaseDTO<ChatMessageDTO> response = new BaseDTO<>(HttpStatus.CREATED.value(), 
                    "Image message created successfully", messageDTO);
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
            
    	} catch (ResourceNotFoundException | BadRequestException e) {
            log.error("Error creating image message: {}", e.getMessage());
    		throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating image message: ", e);
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
            message.setContent("Photo"); // Empty or add content for image messages
            message.setType(EnumMessageType.IMAGE);
            message.setTimestamp(Instant.now());
            message.setImageId(null); // No MongoDB image ID since using URL
            
            // Add image URL to attachments
            Set<String> attachmentUrls = new HashSet<>();
            attachmentUrls.add(imageUrl);
            message.setAttachmentUrls(attachmentUrls);
            
            ChatMessage savedMessage = chatMessageRepository.save(message);
            
            // // Update the chat room's last message
            // chatRoom.setChatMessages(savedMessage);
            // chatRoomRepository.save(chatRoom);

            // Use custom update query instead of full entity save
            chatRoomRepository.updateLastMessageId(chatRoomId, savedMessage.getId());
            log.info("Updated last message for chat room {}", chatRoomId);

            // Send push notifications to users who haven't read the message
            sendPushNotificationsToUnreadUsers(savedMessage);
            
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


    // --- Helper Methods ---
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

    // Helper method to extract URLs from text
    private Set<String> extractUrls(String text) {
        Set<String> urls = new HashSet<>();
        
        // Regex pattern to match URLs
        String urlPattern = "\\b(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)\\b";
        Pattern pattern = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
        
        return urls;
    }

    // Helper method to check if URL is an image
    private boolean isImageUrl(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp|svg)(\\?.*)?$") ||
            lowerUrl.contains("/images/") ||
            lowerUrl.contains("image") && lowerUrl.contains("cdn");
    }

    // Private method to send push notifications
    private void sendPushNotificationsToUnreadUsers(ChatMessage message) {
        try {
            Long senderId = message.getSender().getId();
            Long chatRoomId = message.getChatRooms().getId();
            
            // Get all participants in the chat room except the sender
            List<Long> recipientUserIds = participantRepository
                    .findByChatRoomsId(chatRoomId)
                    .stream()
                    .map(participant -> participant. getUsers().getId())
                    .filter(userId -> ! userId.equals(senderId)) // Exclude sender
                    . collect(Collectors.toList());
            
            if (recipientUserIds.isEmpty()) {
                log.debug("No recipients for push notification in room {}", chatRoomId);
                return;
            }

            // ✅ NEW: Filter out users who are currently ONLINE and in the SAME chat room
            List<Long> usersToNotify = recipientUserIds.stream()
                .filter(userId -> {
                    try {
                        // Check if user is online
                        Participant participant = participantRepository
                                .findByUsersIdAndChatRoomsId(userId, chatRoomId)
                                .orElse(null);
                        
                        if (participant == null) {
                            return true; // Notify if participant not found
                        }
                        
                        // ✅ Don't send notification if user is online (they'll see it in real-time via WebSocket)
                        if (participant.getOnline() == true) {
                            log.debug("Skipping notification for user {} - currently online in room {}", userId, chatRoomId);
                            return false;
                        }
                        
                        return true; // User is offline, send notification
                        
                    } catch (Exception e) {
                        log.error("Error checking user status: {}", e.getMessage());
                        return true; // On error, send notification to be safe
                    }
                })
                .collect(Collectors. toList());
            
            // Filter out users who have already read the message
            // (For new messages, no one has read it yet, so send to all)
            List<Long> usersWithoutReadStatus = filterUsersWithoutReadStatus(
                    message.getId(), 
                    recipientUserIds
            );
            
            if (! usersWithoutReadStatus.isEmpty()) {
                pushNotificationService.sendNewMessageNotification(message, usersWithoutReadStatus);
                log.info("📤 Sent push notifications for message {} to {} users", 
                        message.getId(), usersWithoutReadStatus.size());
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to send push notifications: {}", e.getMessage(), e);
            // Don't throw - notification failure shouldn't fail message creation
        }
    }

    // Filter users who haven't read the message
    private List<Long> filterUsersWithoutReadStatus(Long messageId, List<Long> userIds) {
        // Get users who have READ status for this message
        List<Long> usersWithReadStatus = messageStatusRepository
                .findByMessageIdAndStatus(messageId, EnumStatus.READ)
                .stream()
                .map(status -> status.getUsersReceived().getId())
                .collect(Collectors. toList());
        
        // Return users who don't have READ status
        return userIds.stream()
                .filter(userId -> !usersWithReadStatus.contains(userId))
                .collect(Collectors.toList());
    }
}