package com.project.realtimechat.entity;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@Table(name = "participants")
public class Participant {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_id", nullable = true)
    private User users;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chat_room_id", nullable = true)
	@ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ChatRoom chatRooms;
    
    @Enumerated(EnumType.STRING)
    @NotNull
    private EnumRoomRole role; // Enumerable: ADMIN, MEMBER

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean muted;
    
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean blocked;
    
    @Column(name = "join_date")
    private Instant joinDate;
    
    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;
    
    // Add these fields for online status tracking
    @Column(name = "online", nullable = false, columnDefinition = "boolean default false")
    private boolean online;
    
    @Column(name = "last_seen")
    private Instant lastSeen;
    
    
    // Add these transient fields for user details (not stored in DB)
    @Transient
    private Long userId;
    
    @Transient
    private Long chatRoomId;
    
    @Transient
    private String username;
    
    @Transient
    private String displayName;
    
    // Getters that populate the transient fields for ModelMapper
    public Long getUserId() {
        return users != null ? users.getId() : null;
    }
    
    public Long getChatRoomId() {
        return chatRooms != null ? chatRooms.getId() : null;
    }
    
    public String getUsername() {
        return users != null ? users.getUsername() : null;
    }
    
    public String getDisplayName() {
        return users != null ? users.getDisplayName() : null;
    }
}
