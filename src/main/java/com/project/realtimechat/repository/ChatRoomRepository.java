package com.project.realtimechat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.project.realtimechat.entity.ChatRoom;

import jakarta.persistence.LockModeType;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>{
	@Query("SELECT DISTINCT cr FROM ChatRoom cr " +
		       "JOIN cr.participants p1 " +
		       "JOIN cr.participants p2 " +
		       "WHERE cr.type = 'PERSONAL' " +
		       "AND p1.users.id = :userId1 " +
		       "AND p2.users.id = :userId2 " +
		       "AND p1.id != p2.id")
	List<ChatRoom> findPersonalChatBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
	@Query("SELECT DISTINCT cr FROM ChatRoom cr " +
	           "JOIN cr.participants p " +
	           "WHERE p.users.id = :userId ")
	List<ChatRoom> findChatRoomsByUserId(@Param("userId") Long userId);

	// Custom update query to avoid full entity lock
    @Modifying
    @Transactional
    @Query("UPDATE ChatRoom c SET c.chatMessages.id = :messageId WHERE c.id = :chatRoomId")
    void updateLastMessageId(@Param("chatRoomId") Long chatRoomId, 
                            @Param("messageId") Long messageId);

	// @Lock(LockModeType.PESSIMISTIC_WRITE)
    // @Query("SELECT c FROM ChatRoom c WHERE c.id = :id")
    // Optional<ChatRoom> findByIdWithLock(@Param("id") Long id);
}
