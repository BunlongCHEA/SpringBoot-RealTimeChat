package com.project.realtimechat.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.realtimechat.entity.ChatMessage;
import com.project.realtimechat.entity.EnumMessageType;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatRoomsId(Long chatRoomId);
    
    Page<ChatMessage> findByChatRoomsIdOrderByTimestampDesc(Long chatRoomId, Pageable pageable);
    
    List<ChatMessage> findByChatRoomsIdAndTimestampAfterOrderByTimestampAsc(
            Long chatRoomId, Instant timestamp);
    
    List<ChatMessage> findByChatRoomsIdAndSenderId(Long chatRoomId, Long senderId);
    
    List<ChatMessage> findByChatRoomsIdAndType(Long chatRoomId, EnumMessageType type);
    
    @Query("SELECT m FROM ChatMessage m WHERE m.chatRooms.id = :chatRoomId AND " +
           "LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<ChatMessage> searchByContent(
            @Param("chatRoomId") Long chatRoomId, 
            @Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chatRooms.id = :chatRoomId " +
           "AND m.timestamp > :since AND m.id > " +
           "(SELECT COALESCE(MAX(p.lastReadMessageId), 0) FROM Participant p " +
           "WHERE p.chatRooms.id = :chatRoomId AND p.users.id = :userId)")
    Long countUnreadMessages(
            @Param("chatRoomId") Long chatRoomId, 
            @Param("userId") Long userId,
            @Param("since") Instant since);
    
    @Query("SELECT m FROM ChatMessage m WHERE m.chatRooms.id = :chatRoomId " +
           "AND m.timestamp > :fromTimestamp AND m.timestamp < :toTimestamp " +
           "ORDER BY m.timestamp ASC")
    List<ChatMessage> findMessagesBetweenTimestamps(
            @Param("chatRoomId") Long chatRoomId,
            @Param("fromTimestamp") Instant fromTimestamp,
            @Param("toTimestamp") Instant toTimestamp);
    
    boolean existsByChatRoomsIdAndSenderIdAndId(
            Long chatRoomId, Long senderId, Long messageId);
}