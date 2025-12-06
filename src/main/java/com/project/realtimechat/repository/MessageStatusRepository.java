package com.project.realtimechat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.realtimechat.entity.ChatMessage;
import com.project.realtimechat.entity.EnumStatus;
import com.project.realtimechat.entity.MessageStatus;
import com.project.realtimechat.entity.User;

@Repository
public interface MessageStatusRepository extends JpaRepository<MessageStatus, Long> {
//	@Query("SELECT ms FROM MessageStatus ms WHERE ms.users.id = :userId AND ms.chatMessages.id = :messageId")
	Optional<MessageStatus> findByUsersReceivedIdAndChatMessagesId(Long userReceivedId, Long chatMessageId);
	
	Optional<MessageStatus> findByUsersSentIdAndChatMessagesId(Long userSentId, Long chatMessageId);

	@Query("SELECT ms FROM MessageStatus ms WHERE ms.chatMessages.id = :messageId AND ms.status = :status")
    List<MessageStatus> findByMessageIdAndStatus(@Param("messageId") Long messageId, 
                                                  @Param("status") EnumStatus status);
}
