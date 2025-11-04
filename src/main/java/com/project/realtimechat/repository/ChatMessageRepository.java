package com.project.realtimechat.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.realtimechat.entity.ChatMessage;
import com.project.realtimechat.entity.EnumMessageType;
import com.project.realtimechat.entity.User;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

//    List<ChatMessage> findByChatRoomsId(Long chatRoomId);
	
	Optional<ChatMessage> findById(Long id);
    
    Page<ChatMessage> findByChatRoomsIdOrderByTimestampAsc(Long chatRoomId, Pageable pageable);
    
//    List<ChatMessage> findByChatRoomsIdAndTimestampAfterOrderByTimestampAsc(
//            Long chatRoomId, Instant timestamp);
    
//    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRooms.id = :chatRoomId ORDER BY cm.timestamp ASC")
//    Page<ChatMessage> findByChatRoomsIdOrderByTimestampAsc(@Param("chatRoomId") Long chatRoomId, Pageable pageable);
    
//    @Query("SELECT m FROM ChatMessage m WHERE m.chatRooms.id = :chatRoomId AND m.sender.id = :senderId")
//    List<ChatMessage> findByChatRoomsIdAndSenderId(@Param("chatRoomId") Long chatRoomId, @Param("senderId") Long senderId);
    
//    List<ChatMessage> findByChatRoomsIdAndType(Long chatRoomId, EnumMessageType type);
    
//    @Query("SELECT m FROM ChatMessage m WHERE m.chatRooms.id = :chatRoomId AND " +
//           "LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
//    List<ChatMessage> searchByContent(
//            @Param("chatRoomId") Long chatRoomId, 
//            @Param("searchTerm") String searchTerm);
    
//    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chatRooms.id = :chatRoomId " +
//           "AND m.timestamp > :since AND m.id > " +
//           "(SELECT COALESCE(MAX(p.lastReadMessageId), 0) FROM Participant p " +
//           "WHERE p.chatRooms.id = :chatRoomId AND p.users.id = :userId)")
//    Long countUnreadMessages(
//            @Param("chatRoomId") Long chatRoomId, 
//            @Param("userId") Long userId,
//            @Param("since") Instant since);
    
//    @Query("SELECT m FROM ChatMessage m WHERE m.chatRooms.id = :chatRoomId " +
//           "AND m.timestamp > :fromTimestamp AND m.timestamp < :toTimestamp " +
//           "ORDER BY m.timestamp ASC")
//    List<ChatMessage> findMessagesBetweenTimestamps(
//            @Param("chatRoomId") Long chatRoomId,
//            @Param("fromTimestamp") Instant fromTimestamp,
//            @Param("toTimestamp") Instant toTimestamp);
    
//    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM ChatMessage m WHERE m.chatRooms.id = :chatRoomId AND m.sender.id = :senderId AND m.id = :messageId")
//    boolean existsByChatRoomsIdAndSenderIdAndId(@Param("chatRoomId") Long chatRoomId, @Param("senderId") Long senderId, @Param("messageId") Long messageId);
}