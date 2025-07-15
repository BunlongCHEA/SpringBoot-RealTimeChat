package com.project.realtimechat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.realtimechat.entity.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>{
    @Query("SELECT cr FROM ChatRoom cr " +
           "JOIN cr.participants p1 " +
           "JOIN cr.participants p2 " +
           "WHERE cr.type = 'PERSONAL' " +
           "AND p1.users.id = :userId1 AND p2.users.id = :userId2 " +
           "AND p1.users.id != p2.users.id")
    List<ChatRoom> findPersonalChatBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
	@Query("SELECT DISTINCT cr FROM ChatRoom cr " +
	           "JOIN cr.participants p " +
	           "WHERE p.users.id = :userId ")
	List<ChatRoom> findChatRoomsByUserId(@Param("userId") Long userId);
}
