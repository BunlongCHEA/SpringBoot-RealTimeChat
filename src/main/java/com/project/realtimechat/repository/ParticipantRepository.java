package com.project.realtimechat.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.realtimechat.entity.EnumRoomRole;
import com.project.realtimechat.entity.Participant;

import jakarta.transaction.Transactional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
	
    List<Participant> findByUsersId(Long userId);
    
    List<Participant> findByChatRoomsId(Long chatRoomId);
    
    Optional<Participant> findByUsersIdAndChatRoomsId(Long userId, Long chatRoomId);
    
	List<Participant> findByChatRoomsIdAndRole(Long chatRoomId, EnumRoomRole role);
	
    long countByChatRoomsId(Long chatRoomId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Participant p WHERE p.users.id = :userId AND p.chatRooms.id = :chatRoomId")
    void deleteParticipantByUserAndChatRoom(@Param("userId") Long userId, @Param("chatRoomId") Long chatRoomId);
    
    /**
     * Find chat partners for a specific user
     * This gets all participants from chat rooms where the user participates, excluding the user themselves
     */
    @Query("SELECT DISTINCT p FROM Participant p " +
           "WHERE p.chatRooms.id IN (" +
           "    SELECT p2.chatRooms.id FROM Participant p2 WHERE p2.users.id = :userId" +
           ") AND p.users.id != :userId")
    List<Participant> findChatPartnersByUserId(@Param("userId") Long userId);
    
    /**
     * Find participants in PERSONAL chat rooms only, excluding specific user
     */
    @Query("SELECT DISTINCT p FROM Participant p " +
           "WHERE p.chatRooms.id IN (" +
           "    SELECT p2.chatRooms.id FROM Participant p2 " +
           "    WHERE p2.users.id = :userId AND p2.chatRooms.type = 'PERSONAL'" +
           ") AND p.users.id != :userId")
    List<Participant> findPersonalChatPartnersByUserId(@Param("userId") Long userId);
}
