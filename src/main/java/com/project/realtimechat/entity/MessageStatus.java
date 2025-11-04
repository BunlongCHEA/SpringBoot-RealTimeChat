package com.project.realtimechat.entity;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "message_status")
public class MessageStatus {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne
    @JoinColumn(name = "user_received_id", nullable = false)
    private User usersReceived;
	
	@ManyToOne
    @JoinColumn(name = "user_sent_id", nullable = false)
    private User usersSent;
	
	@ManyToOne
    @JoinColumn(name = "message_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ChatMessage chatMessages;

	@Enumerated(EnumType.STRING)
    @NotNull
    private EnumStatus status; // Enumerable: SENT, DELIVERED, READ

    @NotNull
    private Instant timestamp;
    
    
    // Add these transient fields for user details (not stored in DB)
//    @Transient
//    private Long userId;
//
//    @Transient
//    private Long messageId;

    // Getters that populate the transient fields for ModelMapper

//    public Long getUserId() {
//        return users != null ? users.getId() : null;
//    }
//
//    public Long getMessageId() {
//        return chatMessages != null ? chatMessages.getId() : null;
//    }
}

