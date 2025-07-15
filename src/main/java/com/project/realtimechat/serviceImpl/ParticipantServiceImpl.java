package com.project.realtimechat.serviceImpl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.realtimechat.config.WebSocketConfig;
import com.project.realtimechat.dto.BaseDTO;
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
import com.project.realtimechat.service.ParticipantService;
import com.project.realtimechat.service.UserService;

@Service
public class ParticipantServiceImpl implements ParticipantService {
	private static final Logger log = LoggerFactory.getLogger(ParticipantServiceImpl.class);
	
	private static final String utcString = Instant.now().toString();

    @Autowired
    private ParticipantRepository participantRepository;
    
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ChatRoomService chatRoomService;
    
    @Autowired
    private ModelMapper modelMapper;
    
    /**
     * Retrieves a participant by their ID
     * @param id The ID of the participant to retrieve
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<ParticipantDTO>> getParticipantById(Long id) {
    	try {
            Participant participant = findEntityByParticipantId(id);
            ParticipantDTO participantDTO = modelMapper.map(participant, ParticipantDTO.class);
            
            BaseDTO<ParticipantDTO> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Participant retrieved successfully", participantDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to retrieve participant: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves all participants in a chat room
     * @param chatRoomId The ID of the chat room
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<List<ParticipantDTO>>> getParticipantsByChatRoomId(Long chatRoomId) {
    	 try {
             chatRoomService.findEntityByChatRoomId(chatRoomId);
             
             List<Participant> participants = participantRepository.findByChatRoomsId(chatRoomId);
             List<ParticipantDTO> participantDTOs = participants.stream()
                     .map(participant -> modelMapper.map(participant, ParticipantDTO.class))
                     .collect(Collectors.toList());
             
             BaseDTO<List<ParticipantDTO>> response = new BaseDTO<>(HttpStatus.OK.value(), 
                     "Participants retrieved successfully", participantDTOs);
             
             return new ResponseEntity<>(response, HttpStatus.OK);
         } catch (ResourceNotFoundException e) {
             throw e;
         } catch (Exception e) {
             throw new BadRequestException("Failed to retrieve participants: " + e.getMessage());
         }
    }
    
    /**
     * Retrieves a participant by user ID and chat room ID
     * @param userId The ID of the user
     * @param chatRoomId The ID of the chat room
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<ParticipantDTO>> getParticipantByUserAndChatRoom(Long userId, Long chatRoomId) {
        try {
            Participant participant = participantRepository.findByUsersIdAndChatRoomsId(userId, chatRoomId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Participant not found for user ID: " + userId + " and chat room ID: " + chatRoomId));
            
            ParticipantDTO participantDTO = modelMapper.map(participant, ParticipantDTO.class);
            
            BaseDTO<ParticipantDTO> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Participant retrieved successfully", participantDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to retrieve participant: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves all chat rooms a user participates in
     * @param userId The ID of the user
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<List<ParticipantDTO>>> getParticipantsByUserId(Long userId) {
        try {
            // Verify the user exists
            userService.findEntityByIdUsers(userId);
            
            List<Participant> participants = participantRepository.findByUsersId(userId);
            List<ParticipantDTO> participantDTOs = participants.stream()
                    .map(participant -> modelMapper.map(participant, ParticipantDTO.class))
                    .collect(Collectors.toList());
            
            BaseDTO<List<ParticipantDTO>> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Participants retrieved successfully", participantDTOs);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to retrieve participants: " + e.getMessage());
        }
    }
    
    @Override
    public ResponseEntity<BaseDTO<List<ParticipantDTO>>> getChatPartners(Long userId) {
        try {
            // Use the simpler query method
            List<Participant> chatPartners = participantRepository.findChatPartnersByUserId(userId);
            
            // Convert to DTOs and populate user information
            List<ParticipantDTO> chatPartnerDTOs = chatPartners.stream()
                .map(participant -> {
                    ParticipantDTO dto = modelMapper.map(participant, ParticipantDTO.class);
                    User user = participant.getUsers();
                    if (user != null) {
                        dto.setUserId(user.getId());
                        dto.setUsername(user.getUsername());
                        dto.setFullName(user.getFullName());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
            
            BaseDTO<List<ParticipantDTO>> response = new BaseDTO<>(
                HttpStatus.OK.value(), 
                "Chat partners retrieved successfully", 
                chatPartnerDTOs
            );
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            throw new BadRequestException("Failed to get chat partners: " + e.getMessage());
        }
    }
    
    // Alternative implementation if you want only PERSONAL chat partners
    @Override
    public ResponseEntity<BaseDTO<List<ParticipantDTO>>> getPersonalChatPartners(Long userId) {
        try {
            List<Participant> chatPartners = participantRepository.findPersonalChatPartnersByUserId(userId);
            
            List<ParticipantDTO> chatPartnerDTOs = chatPartners.stream()
                .map(participant -> {
                    ParticipantDTO dto = modelMapper.map(participant, ParticipantDTO.class);
                    User user = participant.getUsers();
                    if (user != null) {
                        dto.setUserId(user.getId());
                        dto.setUsername(user.getUsername());
                        dto.setFullName(user.getFullName());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
            
            BaseDTO<List<ParticipantDTO>> response = new BaseDTO<>(
                HttpStatus.OK.value(), 
                "Personal chat partners retrieved successfully", 
                chatPartnerDTOs
            );
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            throw new BadRequestException("Failed to get personal chat partners: " + e.getMessage());
        }
    }
    
    /**
     * Adds a new participant to a chat room
     * @param chatRoomId The ID of the chat room
     * @param userId The ID of the user to add
     * @param addedByUserId The ID of the user performing the action (must be an ADMIN for non-personal chats)
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<ParticipantDTO>> addParticipantToChatRoom(Long chatRoomId, Long userId, Long addedByUserId) {
        try {
            // Verify the chat room exists
            ChatRoom chatRoom = chatRoomService.findEntityByChatRoomId(chatRoomId);
            
            // Verify the user exists
            User user = userService.findEntityByIdUsers(userId);
            User addedByUser = userService.findEntityByIdUsers(addedByUserId);
            
            // Check if the user is already a participant
            if (participantRepository.findByUsersIdAndChatRoomsId(userId, chatRoomId).isPresent()) {
                throw new BadRequestException("User is already a participant in this chat room");
            }
            
            // For non-personal chats, verify the adding user is an ADMIN
            if (chatRoom.getType() != EnumRoomType.PERSONAL) {
                if (!isUserAdminInChatRoom(addedByUserId, chatRoomId)) {
                    throw new BadRequestException("Only admins can add participants to this chat room");
                }
            } else {
                throw new BadRequestException("Cannot add participants to personal chats");
            }
            
            // Create and save the new participant
            Participant participant = new Participant();
            participant.setUsers(user);
            participant.setChatRooms(chatRoom);
            participant.setRole(EnumRoomRole.MEMBER);
            participant.setMuted(false);
            participant.setBlocked(false);
            participant.setJoinDate(Instant.now());
            
            Participant savedParticipant = participantRepository.save(participant);
            
            // Create a system message announcing the new participant
            // Create a system message announcing the new participant - REUSE FROM CHAT ROOM SERVICE
            String messageContent = addedByUser.getUsername() + " added " + user.getUsername() + " to the " + 
                    (chatRoom.getType() == EnumRoomType.GROUP ? "group" : "channel");
            
            chatRoomService.createSystemMessage(chatRoom, addedByUser, messageContent);
            
            // Convert to DTO and return response
            ParticipantDTO participantDTO = modelMapper.map(savedParticipant, ParticipantDTO.class);
            
            BaseDTO<ParticipantDTO> response = new BaseDTO<>(HttpStatus.CREATED.value(), 
                    "Participant added successfully", participantDTO);
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to add participant: " + e.getMessage());
        }
    }
    
    /**
     * Removes a participant from a chat room
     * @param participantId The ID of the participant to remove
     * @param removedByUserId The ID of the user performing the action (must be an ADMIN for non-personal chats)
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<Void>> removeParticipantFromChatRoom(Long participantId, Long removedByUserId) {
        try {
            // Find the participant
            Participant participant = findEntityByParticipantId(participantId);
            ChatRoom chatRoom = participant.getChatRooms();
            User user = participant.getUsers();
            User removedByUser = userService.findEntityByIdUsers(removedByUserId);
            
            // Check if the removing user has permission
            boolean isSelfRemoval = user.getId().equals(removedByUserId);
            boolean isAdmin = isUserAdminInChatRoom(removedByUserId, chatRoom.getId());
            
            // Users can remove themselves, or ADMINs can remove others
            if (!isSelfRemoval && !isAdmin) {
                throw new BadRequestException("You don't have permission to remove this participant");
            }
            
            // For GROUP/CHANNEL: if removing an ADMIN, check if they're the last ADMIN
            if (!isSelfRemoval && participant.getRole() == EnumRoomRole.ADMIN && 
                chatRoom.getType() != EnumRoomType.PERSONAL) {
                
                long adminCount = participantRepository
                    .findByChatRoomsIdAndRole(chatRoom.getId(), EnumRoomRole.ADMIN).size();
                
                if (adminCount <= 1) {
                    throw new BadRequestException(
                        "Cannot remove the last admin. Assign another admin before removing this user.");
                }
            }
            
            // Create a system message about user leaving/being removed
            String messageContent;
            if (isSelfRemoval) {
                messageContent = user.getUsername() + " left the " + 
                    (chatRoom.getType() == EnumRoomType.GROUP ? "group" : "channel");
            } else {
                messageContent = removedByUser.getUsername() + " removed " + user.getUsername() + " from the " + 
                    (chatRoom.getType() == EnumRoomType.GROUP ? "group" : "channel");
            }
            
            chatRoomService.createSystemMessage(chatRoom, isSelfRemoval ? user : removedByUser, messageContent);
            
            // Remove the participant
            participantRepository.delete(participant);
            
            BaseDTO<Void> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Participant removed successfully", null);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to remove participant: " + e.getMessage());
        }
    }
    
    /**
     * Updates a participant's role
     * @param participantId The ID of the participant
     * @param newRole The new role to assign
     * @param updatedByUserId The ID of the user performing the action (must be an ADMIN)
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<ParticipantDTO>> updateParticipantRole(
            Long participantId, EnumRoomRole newRole, Long updatedByUserId) {
        try {
            // Find the participant
            Participant participant = findEntityByParticipantId(participantId);
            ChatRoom chatRoom = participant.getChatRooms();
            User user = participant.getUsers();
            User updatedByUser = userService.findEntityByIdUsers(updatedByUserId);
            
            // Check if the chat room type supports roles
            if (chatRoom.getType() == EnumRoomType.PERSONAL) {
                throw new BadRequestException("Cannot change roles in personal chats");
            }
            
            // Check if the updating user is an ADMIN
            if (!isUserAdminInChatRoom(updatedByUserId, chatRoom.getId())) {
                throw new BadRequestException("Only admins can update participant roles");
            }
            
            // If demote from ADMIN, check if this is the last ADMIN
            if (participant.getRole() == EnumRoomRole.ADMIN && newRole != EnumRoomRole.ADMIN) {
                long adminCount = participantRepository
                    .findByChatRoomsIdAndRole(chatRoom.getId(), EnumRoomRole.ADMIN).size();
                
                if (adminCount <= 1) {
                    throw new BadRequestException(
                        "Cannot demote the last admin. Promote another user to admin first.");
                }
            }
            
            // Update the role
            participant.setRole(newRole);
            Participant updatedParticipant = participantRepository.save(participant);
            
            // Create a system message about the role change
            String roleChangeMessage = updatedByUser.getUsername() + 
                    (newRole == EnumRoomRole.ADMIN ? " promoted " : " demoted ") + 
                    user.getUsername() + 
                    (newRole == EnumRoomRole.ADMIN ? " to admin" : " to member");
            
            chatRoomService.createSystemMessage(chatRoom, updatedByUser, roleChangeMessage);
            
            ParticipantDTO participantDTO = modelMapper.map(updatedParticipant, ParticipantDTO.class);
            
            BaseDTO<ParticipantDTO> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Participant role updated successfully", participantDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to update participant role: " + e.getMessage());
        }
    }
    
    /**
     * Updates a participant's status (muted/blocked)
     * @param participantId The ID of the participant
     * @param muted Whether to mute the participant
     * @param blocked Whether to block the participant
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<ParticipantDTO>> updateParticipantStatus(
            Long participantId, Boolean muted, Boolean blocked) {
        try {
            // Find the participant
            Participant participant = findEntityByParticipantId(participantId);
            
            // Update status flags if provided
            if (muted != null) {
                participant.setMuted(muted);
            }
            if (blocked != null) {
                participant.setBlocked(blocked);
            }
            
            Participant updatedParticipant = participantRepository.save(participant);
            ParticipantDTO participantDTO = modelMapper.map(updatedParticipant, ParticipantDTO.class);
            
            BaseDTO<ParticipantDTO> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Participant status updated successfully", participantDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to update participant status: " + e.getMessage());
        }
    }
    
    /**
     * Updates a participant's last read message ID
     * @param userId The ID of the user
     * @param chatRoomId The ID of the chat room
     * @param messageId The ID of the last read message
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<ParticipantDTO>> updateLastReadMessageId(
            Long userId, Long chatRoomId, Long messageId) {
        try {
            // Find the participant
            Optional<Participant> participantOpt = participantRepository.findByUsersIdAndChatRoomsId(userId, chatRoomId);
            
            if (!participantOpt.isPresent()) {
                throw new ResourceNotFoundException(
                        "Participant not found for user ID: " + userId + " and chat room ID: " + chatRoomId);
            }
            
            Participant participant = participantOpt.get();
            
            // Verify the message exists and belongs to this chat room
            if (messageId != null) {
                ChatMessage message = chatMessageRepository.findById(messageId)
                        .orElseThrow(() -> new ResourceNotFoundException("Message not found with ID: " + messageId));
                
                if (!message.getChatRooms().getId().equals(chatRoomId)) {
                    throw new BadRequestException("Message does not belong to this chat room");
                }
                
                participant.setLastReadMessageId(messageId);
            } else {
                participant.setLastReadMessageId(null);
            }
            
            Participant updatedParticipant = participantRepository.save(participant);
            ParticipantDTO participantDTO = modelMapper.map(updatedParticipant, ParticipantDTO.class);
            
            BaseDTO<ParticipantDTO> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Last read message updated successfully", participantDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to update last read message: " + e.getMessage());
        }
    }
    
    /**
     * Updates a user's online status across all chat rooms
     * @param userId The ID of the user
     * @param online The online status to set
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<ParticipantDTO>> updateOnlineStatus(Long userId, boolean online) {
        try {
            // Verify the user exists
            User user = userService.findEntityByIdUsers(userId);
            
            // Note: In a real implementation, you would typically store online status
            // in a separate Redis cache or similar fast storage, not in the participant record.
            // This is a simplified implementation.
            
            // For the purpose of this example, we'll just return the status in the DTO
            ParticipantDTO participantDTO = new ParticipantDTO();
            participantDTO.setUserId(userId);
            participantDTO.setOnline(online);
            
            if (!online) {
                participantDTO.setLastSeen(Instant.now());
            }
            
            BaseDTO<ParticipantDTO> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Online status updated successfully", participantDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to update online status: " + e.getMessage());
        }
    }
    
    /**
     * Updates a user's last seen timestamp
     * @param userId The ID of the user
     * @param lastSeen The last seen timestamp
     */
    @Override
    @Transactional
    public ResponseEntity<BaseDTO<ParticipantDTO>> updateLastSeen(Long userId, LocalDateTime lastSeen) {
        try {
            // Verify the user exists
            User user = userService.findEntityByIdUsers(userId);
            
            // Note: In a real implementation, last seen would typically be stored
            // in a separate table or cache, not in the participant record.
            // This is a simplified implementation.
            
            // For the purpose of this example, we'll just return the status in the DTO
            ParticipantDTO participantDTO = new ParticipantDTO();
            participantDTO.setUserId(userId);
            participantDTO.setOnline(false);
            participantDTO.setLastSeen(Instant.now());
            
            BaseDTO<ParticipantDTO> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Last seen updated successfully", participantDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to update last seen: " + e.getMessage());
        }
    }
    
    /**
     * Finds a participant entity by ID
     * @param id The ID of the participant
     */
    @Override
    @Transactional(readOnly = true)
    public Participant findEntityByParticipantId(Long id) {
        try {
            return participantRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Participant not found with ID: " + id));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to find participant: " + e.getMessage());
        }
    }
    
    /**
     * Checks if a user is an ADMIN in a chat room
     * @param userId The ID of the user
     * @param chatRoomId The ID of the chat room
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isUserAdminInChatRoom(Long userId, Long chatRoomId) {
        try {
            Optional<Participant> participant = participantRepository.findByUsersIdAndChatRoomsId(userId, chatRoomId);
            
            log.debug("[{}] | Checking admin status for user {} in chat room {}", 
                    utcString, userId, chatRoomId);
            
            return participant.isPresent() && 
                   participant.get().getRole() == EnumRoomRole.ADMIN;
        } catch (Exception e) {
            throw new BadRequestException("Failed to check admin status: " + e.getMessage());
        }
    }
    
    /**
     * Checks if a user is in a chat room
     * @param userId The ID of the user
     * @param chatRoomId The ID of the chat room
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isUserInChatRoom(Long userId, Long chatRoomId) {
        try {
            return participantRepository.findByUsersIdAndChatRoomsId(userId, chatRoomId).isPresent();
        } catch (Exception e) {
            throw new BadRequestException("Failed to check participant status: " + e.getMessage());
        }
    }
}