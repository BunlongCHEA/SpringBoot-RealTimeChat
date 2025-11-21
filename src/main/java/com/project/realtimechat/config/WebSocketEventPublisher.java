package com.project.realtimechat.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import com.project.realtimechat.dto.ChatMessageDTO;
import com.project.realtimechat.dto.ChatRoomDTO;
import com.project.realtimechat.dto.ParticipantDTO;
import com.project.realtimechat.entity.ChatRoom;
import com.project.realtimechat.entity.EnumStatus;
import com.project.realtimechat.entity.Participant;
import com.project.realtimechat.entity.User;
import com.project.realtimechat.repository.ChatRoomRepository;
import com.project.realtimechat.repository.UserRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WebSocketEventPublisher {
 private static final Logger log = LoggerFactory.getLogger(WebSocketEventPublisher.class);
 
 @Autowired
 private SimpMessageSendingOperations messagingTemplate;
 
 @Autowired
 private UserRepository userRepository;
 
 @Autowired
 private SimpUserRegistry userRegistry;
 
 @Autowired
 private ChatRoomRepository chatRoomRepository;
 
 /**
  * Broadcasts new chat room creation to all participants
  */
 public void broadcastNewChatRoom(ChatRoomDTO chatRoom) {
     try {
         String utcString = Instant.now().toString();
         
         Map<String, Object> notification = new HashMap<>();
         notification.put("type", "NEW_CHAT_ROOM");
         notification.put("chatRoom", chatRoom);
         notification.put("timestamp", utcString);
         
         // Broadcast to ALL ONLINE USERS (not just participants)
         try {
   	      // Option 1: Broadcast to all users (if you want global visibility)
   	      messagingTemplate.convertAndSend("/topic/chat-updates", notification);
   	      
   	      log.info("Broadcasted to /topic/chat-updates for all users");
   	      
   	     } catch (Exception e) {
   	      log.error("Failed to broadcast to all users: {}", e.getMessage());
   	     }
         
//         if (chatRoom.getParticipants() != null && !chatRoom.getParticipants().isEmpty()) {
//             for (ParticipantDTO participant : chatRoom.getParticipants()) {
//                 try {
//                     User user = userRepository.findById(participant.getUserId()).orElse(null);
//                     if (user != null) {
//                         messagingTemplate.convertAndSendToUser(
//                             user.getUsername(),
//                             "/queue/chat-updates",
//                             notification
//                         );
//                     } else {
//                         log.warn("User not found for participant ID: {}", participant.getUserId());
//                     }
//                 } catch (Exception e) {
//                     log.error("Failed to send chat room notification to participant {}: {}", 
//                             participant.getUserId(), e.getMessage());
//                 }
//             }
//             
//             log.debug("Completed broadcasting new chat room {} to {} participants", 
//                     chatRoom.getId(), chatRoom.getParticipants().size());
//             
//         } else {
//             log.warn("No participants found for chat room {}", chatRoom.getId());
//         }
     } catch (Exception e) {
         log.error("Failed to broadcast new chat room: {}", e.getMessage());
     }
 }
 
 /**
  * Broadcasts new participant addition to existing chat room members
  */
 public void broadcastParticipantAdded(Long chatRoomId, ParticipantDTO newParticipant, String addedByUsername) {
     try {
         String utcString = Instant.now().toString();
         log.info("[{}] | Broadcasting participant addition to chat room {}", utcString, chatRoomId);
         
         Map<String, Object> notification = new HashMap<>();
         notification.put("type", "PARTICIPANT_ADDED");
         notification.put("chatRoomId", chatRoomId);
         notification.put("participant", newParticipant);
         notification.put("addedBy", addedByUsername);
         notification.put("timestamp", utcString);
         
         messagingTemplate.convertAndSend(
             "/topic/chat/" + chatRoomId + "/updates",
             notification
         );
         
         User newUser = userRepository.findById(newParticipant.getUserId()).orElse(null);
         if (newUser != null) {
             Map<String, Object> personalNotification = new HashMap<>();
             personalNotification.put("type", "ADDED_TO_CHAT_ROOM");
             personalNotification.put("chatRoomId", chatRoomId);
             personalNotification.put("addedBy", addedByUsername);
             personalNotification.put("timestamp", utcString);
             
             messagingTemplate.convertAndSendToUser(
                 newUser.getUsername(),
                 "/queue/chat-updates",
                 personalNotification
             );
         }
         
     } catch (Exception e) {
         log.error("Failed to broadcast participant addition: {}", e.getMessage());
     }
 }
 
 /**
  * Broadcasts user status updates to other users in the same chat rooms
  */
 public void broadcastUserStatusUpdate(Long userId, String username, boolean online, List<ParticipantDTO> userParticipations) {
     try {
    	 String utcString = Instant.now().toString();
    	 
         Map<String, Object> statusUpdate = new HashMap<>();
         statusUpdate.put("userId", userId);
         statusUpdate.put("username", username);
         statusUpdate.put("online", online);
         statusUpdate.put("lastSeen", utcString);
         statusUpdate.put("type", "USER_STATUS_UPDATE");
         
         // Broadcast to each room the user participates in
         for (ParticipantDTO participation : userParticipations) {
             String destination = "/topic/chat/" + participation.getChatRoomId() + "/status";
             messagingTemplate.convertAndSend(destination, statusUpdate);
             
             log.debug("Broadcasted status update for user {} to chat room {}", 
                     username, participation.getChatRoomId());
         }
         
         // IMPORTANT: Also broadcast to a global user status topic
         // This allows other users to receive updates even if they're in different rooms
         messagingTemplate.convertAndSend("/topic/user/" + userId + "/status", statusUpdate);
         
     } catch (Exception e) {
         log.error("Failed to broadcast status update for user {}: {}", 
                 username, e.getMessage());
     }
 }
 
 /**
  * Broadcasts message status updates
  */
 public void broadcastMessageStatus(Long messageId, Long userId, EnumStatus status, Long chatRoomId) {
     try {
         String utcString = Instant.now().toString();
         
         Map<String, Object> statusUpdate = new HashMap<>();
         statusUpdate.put("type", "MESSAGE_STATUS_UPDATE");
         statusUpdate.put("messageId", messageId);
         statusUpdate.put("userId", userId);
         statusUpdate.put("status", status);
         statusUpdate.put("timestamp", utcString);
         
         messagingTemplate.convertAndSend(
             "/topic/chat/" + chatRoomId + "/status",
             statusUpdate
         );
         
         log.debug("[{}] | Broadcasted message status {} for message {} by user {}", 
                 utcString, status, messageId, userId);
                 
     } catch (Exception e) {
         log.error("Failed to broadcast message status: {}", e.getMessage());
     }
 }
 
 /**
  * Broadcast message sent notification to all participants for sidebar updates
  */
 public void broadcastMessageSentNotification(Long chatRoomId, ChatMessageDTO message) {
    try {
        String utcString = Instant.now().toString();
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "MESSAGE_SENT");
        notification.put("chatRoomId", chatRoomId);
        notification.put("messageId", message.getId());
        notification.put("content", message.getContent());
        notification.put("timestamp", utcString);

        try {
            // Broadcast to all users (if you want global visibility)
            // messagingTemplate.convertAndSend("/topic/message-notifications", notification);
            messagingTemplate.convertAndSend("/topic/message-notifications", message);
            
            log.info("Broadcasted message to /topic/message-notifications - RoomId: {}, Type: {}, MessageId: {}, SenderId: {}, Content: {}", 
                    chatRoomId, 
                    message.getType(), 
                    message.getId(), 
                    message.getSenderId(),
                    message.getContent() != null ? message.getContent().substring(0, Math.min(50, message.getContent().length())) : "no content");
        
        } catch (Exception e) {
            log.error("Failed to broadcast to all users: {}", e.getMessage());
        }
        
    //  // Get all participants of the room
    //  ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElse(null);
    //  if (chatRoom != null && chatRoom.getParticipants() != null) {
    //      for (Participant participant : chatRoom.getParticipants()) {
    //          User user = participant.getUsers();
    //          if (user != null) {
    //              // Send to user's global message notification queue
    //              messagingTemplate.convertAndSendToUser(
    //                  user.getUsername(),
    //                  "/queue/message-notifications",
    //                  notification
    //              );
    //          }
    //      }
    //  }
        
    } catch (Exception e) {
        log.error("Failed to broadcast message sent notification: {}", e.getMessage());
    }
 }
}