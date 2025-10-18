package com.project.realtimechat.serviceImpl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.ChatRoomDTO;
import com.project.realtimechat.dto.ParticipantDTO;
import com.project.realtimechat.entity.ChatMessage;
import com.project.realtimechat.entity.ChatRoom;
import com.project.realtimechat.entity.EnumMessageType;
import com.project.realtimechat.entity.EnumRoomRole;
import com.project.realtimechat.entity.EnumRoomType;
import com.project.realtimechat.entity.Participant;
import com.project.realtimechat.entity.User;
import com.project.realtimechat.exception.BadRequestException;
import com.project.realtimechat.exception.ResourceNotFoundException;
import com.project.realtimechat.repository.ChatMessageRepository;
import com.project.realtimechat.repository.ChatRoomRepository;
import com.project.realtimechat.repository.ParticipantRepository;
import com.project.realtimechat.service.ChatRoomService;
import com.project.realtimechat.service.UserService;

@Service
public class ChatRoomServiceImpl implements ChatRoomService {
	@Autowired
    private ModelMapper modelMapper;
	
	@Autowired
    private ChatRoomRepository chatRoomRepository;
	
	@Autowired
    private ParticipantRepository participantRepository;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private UserService userService;
    
    private static final String utcString = Instant.now().toString();
    
	/**
	 * Get chat room Id
	 * @param id get id of the room
	 */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<ChatRoomDTO>> getChatRoomById(Long id) {
    	try {
            ChatRoom chatRoom = findEntityByChatRoomId(id);
            ChatRoomDTO chatRoomDTO = modelMapper.map(chatRoom, ChatRoomDTO.class);
            
            // Get participants and map them using ModelMapper
            List<Participant> participants = participantRepository.findByChatRoomsId(chatRoom.getId());
            Set<ParticipantDTO> participantDTOs = participants.stream()
                    .map(participant -> modelMapper.map(participant, ParticipantDTO.class))
                    .collect(Collectors.toSet());
            
            chatRoomDTO.setParticipants(participantDTOs);
            
            // Set last message details if available
            if (chatRoom.getChatMessages() != null) {
                populateLastMessageDetails(chatRoomDTO, chatRoom.getChatMessages());
            }
            
            BaseDTO<ChatRoomDTO> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Chat room retrieved successfully", chatRoomDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to retrieve chat room: " + e.getMessage());
        }
    }
    
    /**
	 * Get all chat room by user/participant
	 */
    @Override
    public ResponseEntity<BaseDTO<List<ChatRoomDTO>>> getChatRoomsByUserId(Long userId) {
        try {
            User currentUser = userService.findEntityByIdUsers(userId);
            
            // Get all chat rooms where the user is a participant
            List<ChatRoom> userChatRooms = chatRoomRepository.findChatRoomsByUserId(userId);
            
            // Convert to DTOs and populate last message info
            List<ChatRoomDTO> chatRoomDTOs = userChatRooms.stream()
                .map(chatRoom -> {
                    ChatRoomDTO dto = modelMapper.map(chatRoom, ChatRoomDTO.class);
                    
                    // Get participants for this chat room, excluding current user
                    List<Participant> participants = participantRepository.findByChatRoomsId(chatRoom.getId());
                    Set<ParticipantDTO> participantDTOs = participants.stream()
//                        .filter(p -> !p.getUsers().getId().equals(userId)) // Exclude current user
                        .map(participant -> {
                            ParticipantDTO pDto = modelMapper.map(participant, ParticipantDTO.class);
                            User user = participant.getUsers();
                            if (user != null) {
                                pDto.setUserId(user.getId());
                                pDto.setUsername(user.getUsername());
                                pDto.setFullName(user.getFullName());
//                                pDto.setEmail(user.getEmail());
//                                pDto.setAvatarUrl(user.getAvatarUrl());
                            }
                            return pDto;
                        })
                        .collect(Collectors.toSet());
                    
                    dto.setParticipants(participantDTOs);
                    
                    // Populate last message details
                    if (chatRoom.getChatMessages() != null) {
                        populateLastMessageDetails(dto, chatRoom.getChatMessages());
                    }
                    
                    return dto;
                })
                .collect(Collectors.toList());
            
            BaseDTO<List<ChatRoomDTO>> response = new BaseDTO<>(
                HttpStatus.OK.value(),
                "Chat rooms retrieved successfully",
                chatRoomDTOs
            );
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            throw new BadRequestException("Failed to get chat rooms: " + e.getMessage());
        }
    }
    
	/**
	 * Get all chat room if need to find all
	 */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<List<ChatRoomDTO>>> getAllChatRooms() {
    	try {
            List<ChatRoomDTO> chatRoomDTOs = chatRoomRepository.findAll().stream()
                    .map(chatRoom -> {
                        ChatRoomDTO dto = modelMapper.map(chatRoom, ChatRoomDTO.class);
                        
                        // Get participants and map them using ModelMapper
                        List<Participant> participants = participantRepository.findByChatRoomsId(chatRoom.getId());
                        Set<ParticipantDTO> participantDTOs = participants.stream()
                                .map(participant -> modelMapper.map(participant, ParticipantDTO.class))
                                .collect(Collectors.toSet());
                        
                        dto.setParticipants(participantDTOs);
                        
                        // Set last message details if available
                        if (chatRoom.getChatMessages() != null) {
                            populateLastMessageDetails(dto, chatRoom.getChatMessages());
                        }
                        
                        return dto;
                    })
                    .collect(Collectors.toList());
            
            BaseDTO<List<ChatRoomDTO>> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Chat rooms retrieved successfully", chatRoomDTOs);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            throw new BadRequestException("Failed to retrieve chat rooms: " + e.getMessage());
        }
    }
    
    /**
     * Create a new chat room
     * @param chatRoomDTO The data for the new chat room
     * @param currentUserId The ID of the user creating the chat room
     * @return Response with the created chat room
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<ChatRoomDTO>> createChatRoom(ChatRoomDTO chatRoomDTO, Long currentUserId) {
        try {
            User currentUser = userService.findEntityByIdUsers(currentUserId);
            
            ChatRoom chatRoom;
            boolean isNewRoom = true;
            
            switch (chatRoomDTO.getType()) {
                case PERSONAL:
                    chatRoom = createPersonalChatRoom(chatRoomDTO, currentUser);
                    // Check if it's an existing room
                    isNewRoom = chatRoom.getCreatedUserId().getId().equals(currentUserId) && 
                              chatRoom.getParticipants().size() == 2;
                    break;
                case GROUP:
                    chatRoom = createGroupChatRoom(chatRoomDTO, currentUser);
                    break;
                case CHANNEL:
                    chatRoom = createChannelChatRoom(chatRoomDTO, currentUser);
                    break;
                default:
                    throw new BadRequestException("Invalid chat room type");
            }
            
            // Only create system message for new rooms and non-personal types
            if (isNewRoom && chatRoom.getType() != EnumRoomType.PERSONAL) {
                String systemMessage = getCreateRoomMessage(chatRoom, currentUser);
                if (systemMessage != null) {
                    createSystemMessage(chatRoom, currentUser, systemMessage);
                    chatRoom = chatRoomRepository.save(chatRoom);
                }
            }
            
            // Convert to DTO
            ChatRoomDTO chatRoomResponseDTO = modelMapper.map(chatRoom, ChatRoomDTO.class);
            
            // Set participants
            List<Participant> participants = participantRepository.findByChatRoomsId(chatRoom.getId());
            Set<ParticipantDTO> participantDTOs = participants.stream()
                .map(participant -> {
                    ParticipantDTO pDto = modelMapper.map(participant, ParticipantDTO.class);
                    User user = participant.getUsers();
                    if (user != null) {
                        pDto.setUserId(user.getId());
                        pDto.setUsername(user.getUsername());
                        pDto.setFullName(user.getFullName());
                    }
                    return pDto;
                })
                .collect(Collectors.toSet());

            chatRoomResponseDTO.setParticipants(participantDTOs);
            
            // Set last message details if available
            if (chatRoom.getChatMessages() != null) {
                populateLastMessageDetails(chatRoomResponseDTO, chatRoom.getChatMessages());
            }
            
            String message = isNewRoom ? "Chat room created successfully" : "Found existing chat room";
            BaseDTO<ChatRoomDTO> response = new BaseDTO<>(HttpStatus.OK.value(), message, chatRoomResponseDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to create chat room: " + e.getMessage());
        }
    }

    /**
     * Create a personal chat room between two users
     * @param chatRoomDTO The chat room data
     * @param currentUser The user creating the chat room
     * @return The created chat room entity
     */
    private ChatRoom createPersonalChatRoom(ChatRoomDTO chatRoomDTO, User currentUser) {
        // For PERSONAL chat rooms, need exactly one other user
        if (chatRoomDTO.getParticipants() == null || chatRoomDTO.getParticipants().size() != 1) {
            throw new BadRequestException("PERSONAL chat rooms require exactly one other participant");
        }

        Long otherUserId = chatRoomDTO.getParticipants().iterator().next().getUserId();
        if (otherUserId.equals(currentUser.getId())) {
            throw new BadRequestException("Cannot create a personal chat with yourself");
        }

        // Verify the other user exists
        User otherUser = userService.findEntityByIdUsers(otherUserId);
        if (otherUser == null) {
            throw new ResourceNotFoundException("User with ID " + otherUserId + " not found");
        }

        // Check for existing room between both users - FIXED
        List<ChatRoom> existingRooms = chatRoomRepository.findPersonalChatBetweenUsers(currentUser.getId(), otherUserId);
        
        if (!existingRooms.isEmpty()) {
            ChatRoom existingRoom = existingRooms.get(0);
            
            // Verify the room has exactly 2 participants and they are the correct users
            List<Participant> participants = participantRepository.findByChatRoomsId(existingRoom.getId());
            if (participants.size() == 2) {
                Set<Long> participantIds = participants.stream()
                    .map(p -> p.getUsers().getId())
                    .collect(Collectors.toSet());
                
                if (participantIds.contains(currentUser.getId()) && participantIds.contains(otherUserId)) {
                    return existingRoom;
                }
            }
        }

        // Create new room using transaction to prevent race conditions
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setType(EnumRoomType.PERSONAL);
        chatRoom.setCreatedUserId(currentUser);
        chatRoom.setName(otherUser.getUsername());

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // Add current user as participant
        Participant currentUserParticipant = new Participant();
        currentUserParticipant.setUsers(currentUser);
        currentUserParticipant.setChatRooms(savedChatRoom);
        currentUserParticipant.setRole(EnumRoomRole.MEMBER);
        currentUserParticipant.setJoinDate(Instant.now());
        currentUserParticipant.setMuted(false);
        currentUserParticipant.setBlocked(false);

        participantRepository.save(currentUserParticipant);

        // Add other user as participant
        Participant otherUserParticipant = new Participant();
        otherUserParticipant.setUsers(otherUser);
        otherUserParticipant.setChatRooms(savedChatRoom);
        otherUserParticipant.setRole(EnumRoomRole.MEMBER);
        otherUserParticipant.setJoinDate(Instant.now());
        otherUserParticipant.setMuted(false);
        otherUserParticipant.setBlocked(false);

        participantRepository.save(otherUserParticipant);

        // Refresh the chat room to get the updated participants
        savedChatRoom = chatRoomRepository.findById(savedChatRoom.getId()).orElse(savedChatRoom);

        return savedChatRoom;
    }

    /**
     * Create a group chat room
     * @param chatRoomDTO The chat room data
     * @param currentUser The user creating the chat room
     * @return The created chat room entity
     */
    private ChatRoom createGroupChatRoom(ChatRoomDTO chatRoomDTO, User currentUser) {
        // For GROUP chat rooms, name is required
        if (chatRoomDTO.getName() == null || chatRoomDTO.getName().trim().isEmpty()) {
            throw new BadRequestException("GROUP chat rooms require a name");
        }
        
        // Create the chat room
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setType(EnumRoomType.GROUP);
        chatRoom.setName(chatRoomDTO.getName());
        chatRoom.setCreatedUserId(currentUser);
        
        // Save the chat room first to get an ID
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        
        // Add the creator as admin
        Participant creatorParticipant = new Participant();
        creatorParticipant.setUsers(currentUser);
        creatorParticipant.setChatRooms(chatRoom);
        creatorParticipant.setRole(EnumRoomRole.ADMIN);
        creatorParticipant.setJoinDate(Instant.now());
        creatorParticipant.setMuted(false);
        creatorParticipant.setBlocked(false);
        
        // Save the creator participant
        participantRepository.save(creatorParticipant);
        savedChatRoom.getParticipants().add(creatorParticipant);
        
        // Add other participants if specified
        if (chatRoomDTO.getParticipants() != null && !chatRoomDTO.getParticipants().isEmpty()) {
            for (ParticipantDTO participantDTO : chatRoomDTO.getParticipants()) {
                if (participantDTO.getUserId().equals(currentUser.getId())) {
                    // Skip the creator as they're already added
                    continue;
                }
                
                User user = userService.findEntityByIdUsers(participantDTO.getUserId());
                
                Participant participant = new Participant();
                participant.setUsers(user);
                participant.setChatRooms(savedChatRoom);
                participant.setRole(EnumRoomRole.MEMBER);
                participant.setJoinDate(Instant.now());
                participant.setMuted(false);
                participant.setBlocked(false);
                
                // Save the participant
                participantRepository.save(participant);
                savedChatRoom.getParticipants().add(participant);
            }
        }
        
        return savedChatRoom;
    }

    /**
     * Create a channel chat room
     * @param chatRoomDTO The chat room data
     * @param currentUser The user creating the chat room
     * @return The created chat room entity
     */
    private ChatRoom createChannelChatRoom(ChatRoomDTO chatRoomDTO, User currentUser) {
    	// For CHANNEL chat rooms, name is required
        if (chatRoomDTO.getName() == null || chatRoomDTO.getName().trim().isEmpty()) {
            throw new BadRequestException("CHANNEL chat rooms require a name");
        }
        
        // Create the chat room
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setType(EnumRoomType.CHANNEL);
        chatRoom.setName(chatRoomDTO.getName());
        chatRoom.setCreatedUserId(currentUser);
        
        // Save the chat room first to get an ID
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        
        // Add the creator as admin
        Participant creatorParticipant = new Participant();
        creatorParticipant.setUsers(currentUser);
        creatorParticipant.setChatRooms(savedChatRoom);
        creatorParticipant.setRole(EnumRoomRole.ADMIN);
        creatorParticipant.setJoinDate(Instant.now());
        creatorParticipant.setMuted(false);
        creatorParticipant.setBlocked(false);
        
        // Save the creator participant
        participantRepository.save(creatorParticipant);
        savedChatRoom.getParticipants().add(creatorParticipant);
        
        // Add other participants if specified
        if (chatRoomDTO.getParticipants() != null && !chatRoomDTO.getParticipants().isEmpty()) {
            for (ParticipantDTO participantDTO : chatRoomDTO.getParticipants()) {
                if (participantDTO.getUserId().equals(currentUser.getId())) {
                    // Skip the creator as they're already added
                    continue;
                }
                
                User user = userService.findEntityByIdUsers(participantDTO.getUserId());
                
                Participant participant = new Participant();
                participant.setUsers(user);
                participant.setChatRooms(savedChatRoom);
                // For channels, we might have different roles for subscribers
                participant.setRole(participantDTO.getRole() != null ? participantDTO.getRole() : EnumRoomRole.MEMBER);
                participant.setJoinDate(Instant.now());
                participant.setMuted(false);
                participant.setBlocked(false);
                
                // Save the participant
                participantRepository.save(participant);
                savedChatRoom.getParticipants().add(participant);
            }
        }
        
        return savedChatRoom;
    }
    
    /**
     * Helper method to generate room creation message based on room type
     * @param chatRoom The chat room
     * @param creator The user who created the chat room
     */
    private String getCreateRoomMessage(ChatRoom chatRoom, User creator) {
        switch (chatRoom.getType()) {
            case PERSONAL:
            	// Generate smart date separator instead of "Chat started"
                return generateDateSeparator(Instant.now());
                
            case GROUP:
                return creator.getUsername() + " created group \"" + chatRoom.getName() + "\"";
                
            case CHANNEL:
                return creator.getUsername() + " created channel \"" + chatRoom.getName() + "\"";
                
            default:
                return "Chat room created";
        }
    }

    /**
     * Create a system message for a chat room
     * @param chatRoom The chat room
     * @param user The user related to the action (creator, updater, etc.)
     * @param content The content of the system message
     */
    @Override
    @Transactional
    public ChatMessage createSystemMessage(ChatRoom chatRoom, User user, String content) {
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setChatRooms(chatRoom);
        systemMessage.setType(EnumMessageType.SYSTEM);
        systemMessage.setTimestamp(Instant.now());
        systemMessage.setContent(content);
        systemMessage.setSender(user);
        
        // Save the system message
        ChatMessage savedMessage = chatMessageRepository.save(systemMessage);
        
        // Set as the last message for the chat room
        chatRoom.setChatMessages(savedMessage);
        
        return savedMessage;
    }
    
    
    /**
     * Update a chat room, only ADMINs are allowed to update chat rooms (except for PERSONAL chats)
     * @param id The ID of the chat room
     * @param chatRoomDTO The updated chat room data
     * @param currentUserId The ID of the user making the update
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<ChatRoomDTO>> updateChatRoom(Long id, ChatRoomDTO chatRoomDTO, Long currentUserId) {
        try {
            ChatRoom existingChatRoom = findEntityByChatRoomId(id);
            
            // Check if current user is a participant and has permissions to update
            Participant currentUserParticipant = participantRepository.findByUsersIdAndChatRoomsId(currentUserId, id)
                .orElseThrow(() -> new BadRequestException("You are not a participant in this chat room"));
                
            // For GROUP and CHANNEL chat rooms, only ADMINs can update them
            if (existingChatRoom.getType() != EnumRoomType.PERSONAL && 
                currentUserParticipant.getRole() != EnumRoomRole.ADMIN) {
                throw new BadRequestException("Only admins can update this chat room");
            }
            
            // Only update fields that are present in the DTO
            if (chatRoomDTO.getName() != null) {
                existingChatRoom.setName(chatRoomDTO.getName());
            }
            
            // Type should not be updated directly for security reasons
            // if (chatRoomDTO.getType() != null) {
            //     existingChatRoom.setType(chatRoomDTO.getType());
            // }
            
            if (chatRoomDTO.getLastMessageId() != null) {
                ChatMessage lastMessage = chatMessageRepository.findById(chatRoomDTO.getLastMessageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Last message not found with id: " + 
                                                                     chatRoomDTO.getLastMessageId()));
                existingChatRoom.setChatMessages(lastMessage);
            }
            
            ChatRoom updatedChatRoom = chatRoomRepository.save(existingChatRoom);
            User currentUser = userService.findEntityByIdUsers(currentUserId);
            
            // Create system message for the update
            String updateMessage = currentUser.getUsername() + 
                                   " updated the " + 
                                   (updatedChatRoom.getType() == EnumRoomType.PERSONAL ? "chat" : 
                                    updatedChatRoom.getType() == EnumRoomType.GROUP ? "group" : "channel") + 
                                   " settings";
            
            createSystemMessage(updatedChatRoom, currentUser, updateMessage);
            
            // Save the room with the new system message
            updatedChatRoom = chatRoomRepository.save(updatedChatRoom);
            
            // Convert to DTO
            ChatRoomDTO updatedChatRoomDTO = modelMapper.map(updatedChatRoom, ChatRoomDTO.class);
            
            // Set last message details if available
            if (updatedChatRoom.getChatMessages() != null) {
                populateLastMessageDetails(updatedChatRoomDTO, updatedChatRoom.getChatMessages());
            }
            
            BaseDTO<ChatRoomDTO> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Chat room updated successfully", updatedChatRoomDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to update chat room: " + e.getMessage());
        }
    }
    
    
	/**
	 * Delete or leave a chat room based on its type
	 * @param id           The ID of the chat room
	 * @param userId       The ID of the user performing the action
	 * @param deleteForAll For PERSONAL chats, whether to delete for all participants
	 */
    @Override
    public ResponseEntity<BaseDTO<Void>> deleteChatRoom(Long id, Long userId, Boolean deleteForAll) {
        try {
            ChatRoom chatRoom = findEntityByChatRoomId(id);
            User user = userService.findEntityByIdUsers(userId);
            
            // Verify that the user is a participant in this chat room
            Participant participant = participantRepository.findByUsersIdAndChatRoomsId(userId, id)
                .orElseThrow(() -> new BadRequestException("User is not a participant in this chat room"));
            
            // Handle based on room type
            switch (chatRoom.getType()) {
                case PERSONAL:
                    handlePersonalChatRoomDeletion(chatRoom, userId, deleteForAll);
                    break;
                    
                case GROUP:
                	handleGroupOrChannelLeave(chatRoom, participant);
                    break;
                    
                case CHANNEL:
                    handleGroupOrChannelLeave(chatRoom, participant);
                    break;
                    
                default:
                    throw new BadRequestException("Unknown room type");
            }
            
            BaseDTO<Void> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Chat room operation completed successfully", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to process chat room operation: " + e.getMessage());
        }
    }
    
	/**
	 * Handle deletion for personal chat rooms (between 2 people)
	 * @param chatRoom     The chat room
	 * @param userId       The ID of the user performing the deletion
	 * @param deleteForAll Whether to delete for both participants
	 */
    private void handlePersonalChatRoomDeletion(ChatRoom chatRoom, Long userId, Boolean deleteForAll) {
    	// Ensure this is actually a personal chat
        if (chatRoom.getType() != EnumRoomType.PERSONAL) {
            throw new BadRequestException("This operation is only valid for personal chats");
        }
        
        // Get all participants
        List<Participant> participants = participantRepository.findByChatRoomsId(chatRoom.getId());
        
        // Ensure there are exactly 2 participants
        if (participants.size() != 2) {
            throw new BadRequestException("Personal chats should have exactly 2 participants");
        }
        
        if (Boolean.TRUE.equals(deleteForAll)) {
            // Delete the entire chat room and all related data
            chatRoomRepository.delete(chatRoom);
        } else {
            // Delete just for the current user (remove them as participant)
            Participant currentUserParticipant = participants.stream()
                .filter(p -> p.getUsers().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
                
            participantRepository.delete(currentUserParticipant);
        }
    }
    
    /**
     * Handle leaving a group or channel chat room
     * @param chatRoom The chat room
     * @param participant The participant who is leaving
     */
    private void handleGroupOrChannelLeave(ChatRoom chatRoom, Participant participant) {
        // Check if this user is the only participant
        long participantCount = participantRepository.countByChatRoomsId(chatRoom.getId());
        
        // Check if this user is the only ADMIN
        boolean isAdmin = EnumRoomRole.ADMIN.equals(participant.getRole());
        long adminCount = 0;
        
        if (isAdmin) {
            adminCount = participantRepository.findByChatRoomsIdAndRole(chatRoom.getId(), EnumRoomRole.ADMIN).size();
        }
        
        if (participantCount == 1) {
            // This is the last participant, delete the entire chat room
            chatRoomRepository.delete(chatRoom);
        } else if (isAdmin && adminCount == 1) {
            // This is the last ADMIN, assign ADMIN role to another participant
        	throw new BadRequestException(
                "You are the last admin of this " + 
                (chatRoom.getType() == EnumRoomType.GROUP ? "group" : "channel") + 
                ". Please assign another participant as admin before leaving."
            );
        } else {
            // Create a system message about the user leaving before removing them
            User leavingUser = participant.getUsers();
            String leaveMessage = leavingUser.getUsername() + 
                                  " has left the " + 
                                  (chatRoom.getType() == EnumRoomType.GROUP ? "group" : "channel");
            
            createSystemMessage(chatRoom, leavingUser, leaveMessage);
            
            // Save the chat room with the new system message
            chatRoomRepository.save(chatRoom);
            
            // Just remove this participant
            participantRepository.delete(participant);
        }
    }
    
    /**
     * Finds and retrieves a ChatRoom entity by its ID to fetch a chat room entity from the repository.
     * @param id The unique identifier of the chat room to find
     */
    @Override
    @Transactional(readOnly = true)
    public ChatRoom findEntityByChatRoomId(Long id) {
        try {
            return chatRoomRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with id: " + id));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to find chat room: " + e.getMessage());
        }
    }
    
    /**
     * Populates a ChatRoomDTO with details from the last or recent message in the chat room including content preview, sender,
     * timestamp, message type, and attachment count.
     * The content preview is formatted differently based on the message type (text, image, file, system).
     * @param chatRoomDTO The DTO to populate with last message details
     * @param lastMessage The most recent ChatMessage entity for the chat room
     */
    private void populateLastMessageDetails(ChatRoomDTO chatRoomDTO, ChatMessage lastMessage) {
        chatRoomDTO.setLastMessageId(lastMessage.getId());
        
        // Set message content based on type
        if (lastMessage.getType() == EnumMessageType.TEXT) {
            chatRoomDTO.setLastMessageContent(lastMessage.getContent());
        } else if (lastMessage.getType() == EnumMessageType.IMAGE) {
            chatRoomDTO.setLastMessageContent("[Image]");
        } else if (lastMessage.getType() == EnumMessageType.FILE) {
            chatRoomDTO.setLastMessageContent("[File]");
        } else if (lastMessage.getType() == EnumMessageType.SYSTEM) {
            chatRoomDTO.setLastMessageContent("[System Message: " + lastMessage.getContent() + "]");
        }
        
        // Set sender username if available
        if (lastMessage.getSender() != null) {
            chatRoomDTO.setLastMessageSenderUsername(lastMessage.getSender().getUsername());
        }
        
     // Set timestamp as Instant directly (no formatting to string)
        if (lastMessage.getTimestamp() != null) {
            chatRoomDTO.setLastMessageTimestamp(lastMessage.getTimestamp());
        } else {
            // Fallback to current time if timestamp is null
            chatRoomDTO.setLastMessageTimestamp(Instant.now());
        }
        
        // Set message type
        chatRoomDTO.setLastMessageType(lastMessage.getType());
        
        // Set attachment count - implementing this from the provided message entity
        if (lastMessage.getAttachmentUrls() != null && !lastMessage.getAttachmentUrls().isEmpty()) {
            chatRoomDTO.setLastMessageAttachmentCount(lastMessage.getAttachmentUrls().size());
        } else {
            chatRoomDTO.setLastMessageAttachmentCount(0);
        }
    }
    
    
    // Add this method to generate smart date separators
    private String generateDateSeparator(Instant timestamp) {
        LocalDateTime messageTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        LocalDateTime now = LocalDateTime.now();
        
        // Calculate difference
        long daysDifference = ChronoUnit.DAYS.between(messageTime.toLocalDate(), now.toLocalDate());
        
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String timeStr = messageTime.format(timeFormatter);
        
        if (daysDifference == 0) {
            return "Today " + timeStr;
        } else if (daysDifference == 1) {
            return "Yesterday " + timeStr;
        } else if (daysDifference <= 7) {
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE");
            return messageTime.format(dayFormatter) + " " + timeStr;
        } else {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return messageTime.format(dateFormatter) + " " + timeStr;
        }
    }
    
    // Add method to check if we should show a date separator
    private boolean shouldShowDateSeparator(Instant lastMessageTime, Instant currentMessageTime) {
        if (lastMessageTime == null) {
            return true; // First message
        }
        
        long hoursDifference = ChronoUnit.HOURS.between(lastMessageTime, currentMessageTime);
        return hoursDifference >= 2;
    }
}
