package com.project.realtimechat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.project.realtimechat.entity.ChatMessage;
import com.project.realtimechat.entity.EnumStatus;
import com.project.realtimechat.entity.MessageStatus;
import com.project.realtimechat.entity.User;

public interface MessageStatusRepository extends JpaRepository<MessageStatus, Long> {
//	@Query("SELECT ms FROM MessageStatus ms WHERE ms.users.id = :userId AND ms.chatMessages.id = :messageId")
	Optional<MessageStatus> findByUsersReceivedIdAndChatMessagesId(Long userReceivedId, Long chatMessageId);
	
	Optional<MessageStatus> findByUsersSentIdAndChatMessagesId(Long userSentId, Long chatMessageId);
	    
//    List<MessageStatus> findByChatMessagesId(Long chatMessageId);
    
//    List<MessageStatus> findByUsersId(Long userId);
    
//    List<MessageStatus> findByUsersIdAndStatus(Long userId, EnumStatus status);
    
//    List<MessageStatus> findByChatMessagesIdAndStatus(Long chatMessageId, EnumStatus status);
}
