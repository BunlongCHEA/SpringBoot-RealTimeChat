package com.project.realtimechat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.realtimechat.entity.EnumRoomRole;
import com.project.realtimechat.entity.Participant;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
	
    List<Participant> findByUsersId(Long userId);
    
    List<Participant> findByChatRoomsId(Long chatRoomId);
    
    Optional<Participant> findByUsersIdAndChatRoomsId(Long userId, Long chatRoomId);
    
	List<Participant> findByChatRoomsIdAndRole(Long chatRoomId, EnumRoomRole role);
	
    long countByChatRoomsId(Long chatRoomId);
}
