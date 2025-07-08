package com.project.realtimechat.dto;

import java.time.Instant;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.realtimechat.entity.EnumRoomRole;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantDTO {
	private Long id;
	private Long userId;
    private Long chatRoomId;
    private EnumRoomRole role;
    private boolean muted;
    private boolean blocked;
    private Instant joinDate;
    private Long lastReadMessageId;
    
    // Online status fields
    private boolean online;
    private Instant lastSeen;
    
    // User details
    private String username;
    private String displayName;
}
